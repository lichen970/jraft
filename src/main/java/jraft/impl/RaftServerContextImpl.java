package jraft.impl;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import javafx.util.Pair;
import jraft.RaftServerContext;
import jraft.proto.AppendEntryRequest;
import jraft.proto.AppendEntryResponse;
import jraft.proto.VoteRequest;
import jraft.proto.VoteResponse;
import jraft.rpc.RpcClient;
import jraft.rpc.RpcServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class RaftServerContextImpl implements RaftServerContext {

    private final Logger logger
            = LoggerFactory.getLogger(RaftServerContextImpl.class);

    private static final long LOWER_BOUND_TIMEOUT = 1000;
    private static final long UPPER_BOUND_TIMEOUT = 10000;
    private static final long HEART_BEAT_INTERVAL = 500;
    private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;
    private final ScheduledExecutorService scheduler;
    private final String name;
    private int voteCounter;
    private volatile String lastVotedFor;
    private volatile long term;
    private volatile String leaderId;
    private ScheduledFuture leaderEvent;
    private ScheduledFuture candidateEvent;
    private ScheduledFuture followerEvent;
    private HashMap<String, RpcClient> peerMap;
    private Role currentRole;

    public RaftServerContextImpl(String name,
                                 ScheduledExecutorService scheduler) {
        this(name, scheduler, null);
    }

    public RaftServerContextImpl(String name,
                                 ScheduledExecutorService scheduler,
                                 Map<String, RpcClient> peers) {
        this.name = Preconditions.checkNotNull(name);
        this.scheduler = Preconditions.checkNotNull(scheduler);
        this.peerMap = new HashMap<>(peers == null ? Collections.emptyMap() : peers);
    }

    public RaftServerContextImpl(String name) {
        this(name, Executors.newScheduledThreadPool(1), null);
    }


    @Override
    public String getServerName() {
        return this.name;
    }

    @Override
    public void setTerm(long toTerm) {
        Preconditions.checkArgument(toTerm >= 0);
        logger.debug("set term from {} to {}", this.term, toTerm);
        this.term = toTerm;
    }

    @Override
    public long getTerm() {
        return this.term;
    }

    @Override
    public void setLastVoteFor(String candidateServerName) {
        Preconditions.checkArgument(candidateServerName != null);
        this.lastVotedFor = candidateServerName;
    }

    @Override
    public String getLastVoteFor() {
        return this.lastVotedFor;
    }

    @Override
    public void setLeaderId(String leaderId) {
        Preconditions.checkArgument(leaderId != null);
        this.leaderId = leaderId;
    }

    @Override
    public String getLeaderId() {
        return this.leaderId;
    }

    @Override
    public void addMemberToPeers(String peerId, RpcClient client) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(peerId));
        Preconditions.checkArgument(client != null);
        this.peerMap.put(peerId, client);
    }

    @Override
    public void removeMemberFromPeers(String peerId) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(peerId));
        this.peerMap.remove(peerId);
    }

    @Override
    public Collection<String> getPeers() {
        return Collections.unmodifiableSet(this.peerMap.keySet());
    }

    @Override
    public void transition(Role newRole) {
        resetState();
        logger.info("role transitioning from {} to {}", getCurrentRole(),
                newRole);
        switch (newRole) {
            case FOLLOWER:
                doAsFollower();
                break;
            case CANDIDATE:
                doAsCandidate();
                break;
            case LEADER:
                doAsLeader();
                break;
            default:
                doAsInactive();
                break;
        }
    }

    private void resetState() {
        logger.debug("cleaning states before transition state...");
        this.voteCounter = 0;
        this.leaderEvent.cancel(true);
        this.candidateEvent.cancel(true);
        this.followerEvent.cancel(true);
    }

    private long getRandomTimeout() {
        return ThreadLocalRandom
                .current()
                .nextLong(LOWER_BOUND_TIMEOUT, UPPER_BOUND_TIMEOUT + 1);
    }

    private void doAsLeader() {
        logger.info("switching to leader...");
        setRole(Role.LEADER);
        this.leaderId = name;
        Preconditions.checkState(getCurrentRole() == Role.LEADER);
        Preconditions.checkState(leaderEvent == null);
        Runnable sendHeartBeat = () -> {
            for (String peerName : peerMap.keySet()) {
                RpcClient client = peerMap.get(peerName);
                // safe check
                Preconditions.checkState(name.equals(leaderId));
                AppendEntryRequest request = AppendEntryRequest
                        .newBuilder()
                        .setTerm(term)
                        .setLeaderId(leaderId)
                        .build();
                AppendEntryResponse response =
                        client.sendAppendRequest(request);
                logger.debug("heart beat response: ", response);
                if (!response.getSuccess()) {
                    logger.warn("heart beat request to peer {} failed! ",
                            peerName);
                } else {
                    logger.info("heart beat request to peer {} succeeded!",
                            peerName);
                }
                // if someone is faster than me, give out leadership
                if (response.getTerm() > term) {
                    logger.debug("peer is ahead!");
                    setTerm(response.getTerm());
                    transition(Role.FOLLOWER);
                }
            }
        };
        leaderEvent = scheduler.scheduleAtFixedRate(
                sendHeartBeat,
                0,
                HEART_BEAT_INTERVAL,
                TIME_UNIT
        );
    }

    private void doAsFollower() {
        logger.info("switching to follower...");
        Preconditions.checkState(followerEvent == null);
        setRole(Role.FOLLOWER);
        Runnable waitForTimeout = () -> transition(Role.CANDIDATE);
        followerEvent = scheduler.schedule(
                waitForTimeout,
                getRandomTimeout(),
                TIME_UNIT
        );
    }

    private void doAsCandidate() {
        logger.info("switching to candidate...");
        Runnable timeoutEvent = () -> transition(Role.CANDIDATE);
        candidateEvent = scheduler.schedule(
                timeoutEvent,
                getRandomTimeout(),
                TIME_UNIT
        );

        for (String peerName : peerMap.keySet()) {
            VoteRequest request = VoteRequest
                    .newBuilder()
                    .setTerm(term)
                    .setCandidateId(name)
                    .build();
            RpcClient client = peerMap.get(peerName);
            VoteResponse response = client.sendVoteRequest(request);
            logger.debug("vote response: {}", response);
            if (response.getVoteGranted()) {
                logger.info("vote granted from peer {}!", peerName);
                voteCounter++;
                logger.debug("vote count after increase: {}", voteCounter);
            } else {
                logger.warn("vote rejected from peer {}", peerName);
            }
            if (response.getTerm() > term) {
                logger.debug("peer is ahead!");
                setTerm(response.getTerm());
                transition(Role.FOLLOWER);
            } else {
                if (2 * voteCounter > peerMap.size() + 1) {
                    setTerm(this.term + 1);
                    transition(Role.LEADER);
                }
            }
        }
    }

    private void doAsInactive() {
        logger.info("switching to inactive...");
        setRole(Role.INACTIVE);
        // TODO: need add some await here to wait for signal.
    }

    // TODO: these probably should also be public. later we need to refactor
    // all the caller of set* calls out of this class and let the class only
    // maintain states.
    private void setRole(Role oldRole, Role newRole) {
        Preconditions.checkState(getCurrentRole() == oldRole);
        this.currentRole = newRole;
    }

    private void setRole(Role newRole) {
        logger.debug("set role from {} to {}", getCurrentRole(), newRole);
        this.currentRole = newRole;
    }

    private Role getCurrentRole() {
        return this.currentRole;
    }

    @Override
    public ScheduledExecutorService getScheduler() {
        return this.scheduler;
    }


    @Override
    public void bootstrap(String selfConnectionString, List<String>
            clusterConnectionStrings) {
        Preconditions.checkNotNull(clusterConnectionStrings);
        logger.info("bootstrap cluster with local member {}/{} and remote " +
                        "peers {}", getServerName(), selfConnectionString,
                clusterConnectionStrings);
        // 1. boot up rpc server first
        Pair<String, Integer> myHostAndPort
                = getHostAndPort(selfConnectionString);
        RpcServer rpcServer = new RpcServer(myHostAndPort.getValue());
        try {
            rpcServer.start();
        } catch (IOException e) {
            logger.warn("not able to start local RPC server");
            e.printStackTrace();
        }
        // 2. tries to connect to all other servers and store clients to map
        for (String connectionString : clusterConnectionStrings) {
            Pair<String, Integer> hostAndPort = getHostAndPort(connectionString);
            RpcClient client
                    = new RpcClient(hostAndPort.getKey(), hostAndPort.getValue());
            logger.debug("established connection to {} via client {}",
                    connectionString, client);
            client.sendVoteRequest(VoteRequest.newBuilder().build());
            peerMap.put(connectionString, client);
        }
        // TODO: initialize log, and meta store later when added.
        // enter into local state machine
        // we can move this method out to server
        //transition(Role.FOLLOWER);
    }

    private Pair<String, Integer> getHostAndPort(String connectionString) {
        Pair<String, Integer> hostAndPort = null;
        if (connectionString == null || connectionString.isEmpty()) {
            return hostAndPort;
        }
        String[] parts = connectionString.split(":");
        hostAndPort = new Pair<>(parts[0], Integer.parseInt(parts[1]));
        return hostAndPort;
    }

    @Override
    public void close() {

    }
}
