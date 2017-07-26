package jraft;

import jraft.rpc.gRpcClient;
import java.util.concurrent.ExecutorService;

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

    void setId(String id);

    String getId();

    void setTerm(long term);

    long getTerm();

    void setLastCandidateIdVoteFor(String lastCandidateIdVoteFor);

    String getLastCandidateIdVoteFor();

    void setLeaderId(String leaderId);

    String getLeaderId();

    void addMemberToPeers(int peerId, gRpcClient client);

    void removeMemberFromPeers(int peerId);

    void transition(Role currentRole);

    ExecutorService getScheduler();
}
