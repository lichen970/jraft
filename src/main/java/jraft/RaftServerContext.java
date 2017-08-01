package jraft;

import jraft.rpc.RpcClient;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

public interface RaftServerContext {
    /**
     * Raft Server State Type
     */
    enum Role {
        INACTIVE,
        CANDIDATE,
        FOLLOWER,
        LEADER,
    }

    String getServerName();

    void setTerm(long term);

    long getTerm();

    void setLastVoteFor(String candidateServerName);

    String getLastVoteFor();

    void setLeaderId(String leaderId);

    String getLeaderId();

    void addMemberToPeers(String peerId, RpcClient client);

    void removeMemberFromPeers(String peerId);

    Collection<String> getPeers();

    void transition(Role currentRole);

    ScheduledExecutorService getScheduler();

    /**
     * boot up server.
     *
     * @param selfConnectionString
     * @param peerConnectionStrings
     */
    void bootstrap(String selfConnectionString, List<String>
            peerConnectionStrings);

    void close();
}
