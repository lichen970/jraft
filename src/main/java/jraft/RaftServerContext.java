package jraft;

import jraft.rpc.gRpcClient;

import java.util.Collection;
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

    void addMemberToPeers(String peerId, gRpcClient client);

    void removeMemberFromPeers(String peerId);

    Collection<String> getPeers();

    void transition(Role currentRole);

    ScheduledExecutorService getScheduler();

    void bootstrap();
}
