package jraft.rpc;

import io.grpc.stub.StreamObserver;
import jraft.impl.RaftServerContextImpl;
import jraft.proto.*;

/**
 * Created by Chen on 7/20/17.
 */
public class RaftRpcImpl extends RaftGrpc.RaftImplBase {
    private RaftServerContextImpl raftServerContextImpl;

    public RaftRpcImpl(RaftServerContextImpl raftServerContextImpl) {
        this.raftServerContextImpl = raftServerContextImpl;
    }

    @Override
    public void requestVote(VoteRequest request,
                            StreamObserver<VoteResponse> responseObserver) {
        VoteResponse response = getVoteResponse(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private VoteResponse getVoteResponse(VoteRequest request) {
        long currentTerm = raftServerContextImpl.getTerm();
        long requestTerm = request.getTerm();
        // TODO: If votedFor is candidateId and candidate's log is at least as
        // as receiver's log, grant vote
        boolean voteGranted = (requestTerm >= currentTerm)
                && (raftServerContextImpl.getLastVoteFor() == null);
        long term = voteGranted ? requestTerm : currentTerm;
        if (voteGranted) {
            raftServerContextImpl.setLastVoteFor(request.getCandidateId());
        }
        return VoteResponse.newBuilder().setTerm(term)
                .setVoteGranted(voteGranted).build();
    }

    @Override
    public void append(AppendEntryRequest request,
                       StreamObserver<AppendEntryResponse> responseObserver) {
        AppendEntryResponse response
                = getAppendResponse(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private AppendEntryResponse getAppendResponse(AppendEntryRequest request) {
        long currentTerm = raftServerContextImpl.getTerm();
        long requestTerm = request.getTerm();
        // TODO: Reply false if log doesn't contain an entry at prevLogIndex
        // whose term matches pervLogTerm
        boolean success = requestTerm >= currentTerm;
        long term = success ? requestTerm : currentTerm;
        // TODO: update entry
        return AppendEntryResponse.newBuilder().setTerm(term)
                .setSuccess(success).build();
    }
}

