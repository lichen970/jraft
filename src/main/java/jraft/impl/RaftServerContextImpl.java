package jraft.impl;

import com.google.common.base.Preconditions;
import jraft.RaftServerContext;
import jraft.proto.AppendEntryRequest;
import jraft.proto.AppendEntryResponse;
import jraft.proto.VoteRequest;
import jraft.proto.VoteResponse;
import jraft.rpc.gRpcClient;

import java.util.HashMap;
import java.util.concurrent.*;

public class RaftServerContextImpl implements RaftServerContext {
    private static final long LOWER_BOUND_TIMEOUT = 1000;
    private static final long UPPER_BOUND_TIMEOUT = 10000;
    private static final long HEART_BEAT_INTERVAL = 500;
    private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;
    private final ScheduledExecutorService scheduler;
    private long term;
    private int voteCounter;
    private String lastCandidateIdVotedFor;
    private String id;
    private String leaderId;
    private ScheduledFuture leaderEvent;
    private ScheduledFuture candidateEvent;
    private ScheduledFuture followerEvent;
    private HashMap<Integer, gRpcClient> peerMap = new HashMap<>();

    public RaftServerContextImpl(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    public RaftServerContextImpl() {
        this(Executors.newScheduledThreadPool(1));
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void setTerm(long term) {
        Preconditions.checkArgument(term >= 0);
        this.term = term;
    }

    @Override
    public long getTerm() {
        return this.term;
    }

    @Override
    public void setLastCandidateIdVoteFor(String lastCandidateIdVoteFor) {
        Preconditions.checkArgument(lastCandidateIdVoteFor != null);
        this.lastCandidateIdVotedFor = lastCandidateIdVoteFor;
    }

    @Override
    public String getLastCandidateIdVoteFor() {
        return this.lastCandidateIdVotedFor;
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
    public void addMemberToPeers(int peerId, gRpcClient client) {
        Preconditions.checkArgument(peerId >= 0);
        Preconditions.checkArgument(client != null);
        this.peerMap.put(peerId, client);
    }

    @Override
    public void removeMemberFromPeers(int peerId) {
        Preconditions.checkArgument(peerId >= 0);
        this.peerMap.remove(peerId);
    }

    @Override
    public void transition(Role currentRole) {
        this.voteCounter = 0;
        this.leaderEvent.cancel(true);
        this.candidateEvent.cancel(true);
        this.followerEvent.cancel(true);
        switch(currentRole) {
            case INACTIVE:
                doAsInactive();
                break;
            case FOLLOWER:
                doAsFollower();
                break;
            case CANDIDATE:
                doAsCandidate();
                break;
            case LEADER:
                doAsLeader();
                break;
        }
    }

    private long getRandomTimeout() {
        return ThreadLocalRandom
                .current()
                .nextLong(LOWER_BOUND_TIMEOUT, UPPER_BOUND_TIMEOUT + 1);
    }

    private void doAsLeader() {
        Runnable sendHeartBeat = () -> {
            for (gRpcClient client: peerMap.values()) {
                AppendEntryRequest request = AppendEntryRequest
                        .newBuilder()
                        .setTerm(term)
                        .setLeaderId(leaderId)
                        .build();
                AppendEntryResponse response =
                        client.sendAppendRequest(request);
                if (response.getSuccess() && response.getTerm() > this.term) {
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
        long timeout = getRandomTimeout();
        Runnable waitForTimeout = () -> transition(Role.CANDIDATE);
        followerEvent = scheduler.schedule(
                waitForTimeout,
                timeout,
                TIME_UNIT
        );
    }

    private void doAsCandidate() {
        long timeout = getRandomTimeout();
        Runnable timeoutEvent = () -> transition(Role.CANDIDATE);
        candidateEvent = scheduler.schedule(
                timeoutEvent,
                timeout,
                TIME_UNIT
        );

        for (gRpcClient client: this.peerMap.values()) {
            VoteRequest request = VoteRequest
                    .newBuilder()
                    .setTerm(term)
                    .setCandidateId(id)
                    .build();
            VoteResponse response = client.sendVoteRequest(request);
            if (response.getVoteGranted()) {
                this.voteCounter++;
            } else if (response.getTerm() > this.term) {
                transition(Role.FOLLOWER);
            }
            if (voteCounter > peerMap.size() / 2) {
                this.term++;
                transition(Role.LEADER);
            }
        }
    }

    private void doAsInactive() {
    }

    @Override
    public ExecutorService getScheduler() {
        return this.scheduler;
    }
}
