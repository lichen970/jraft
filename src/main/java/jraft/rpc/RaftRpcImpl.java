package jraft.rpc;

import com.google.common.base.Preconditions;
import io.grpc.stub.StreamObserver;
import jraft.impl.RaftServerContextImpl;
import jraft.proto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Chen on 7/20/17.
 */
public class RaftRpcImpl extends RaftGrpc.RaftImplBase {

    private final Logger logger = LoggerFactory.getLogger(RaftRpcImpl.class);

    private RaftServerContextImpl raftServerContextImpl;

    public RaftRpcImpl(RaftServerContextImpl raftServerContextImpl) {
        Preconditions.checkArgument(raftServerContextImpl != null);
        this.raftServerContextImpl = raftServerContextImpl;
    }

    @Override
    public void requestVote(VoteRequest request,
                            StreamObserver<VoteResponse> responseObserver) {
        Preconditions.checkArgument(request != null);
        Preconditions.checkArgument(responseObserver != null);
        VoteResponse response = getVoteResponse(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private VoteResponse getVoteResponse(VoteRequest request) {
        Preconditions.checkArgument(request.hasField(request.getDescriptorForType().findFieldByNumber(1)));
        long currentTerm = raftServerContextImpl.getTerm();
        long requestTerm = request.getTerm();
        String lastVoteFor = raftServerContextImpl.getLastVoteFor();
        // TODO: If votedFor is candidateId and candidate's log is at least as receiver's log, grant vote
        boolean voteGranted = (requestTerm >= currentTerm) && (lastVoteFor == null);
        long term = voteGranted ? requestTerm : currentTerm;
        logger.debug("vote granded: {}, current term: {}, last vote for: {}, request term: {}",
                voteGranted, currentTerm, lastVoteFor, requestTerm);
        if (voteGranted) {
            raftServerContextImpl.setLastVoteFor(request.getCandidateId());
        }
        return VoteResponse.newBuilder().setTerm(term)
                .setVoteGranted(voteGranted).build();
    }

    @Override
    public void append(AppendEntryRequest request,
                       StreamObserver<AppendEntryResponse> responseObserver) {
        Preconditions.checkArgument(request != null);
        Preconditions.checkArgument(responseObserver != null);
        AppendEntryResponse response
                = getAppendResponse(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private AppendEntryResponse getAppendResponse(AppendEntryRequest request) {
        Preconditions.checkArgument(request.hasField(request.getDescriptorForType().findFieldByNumber(1)));
        long currentTerm = raftServerContextImpl.getTerm();
        long requestTerm = request.getTerm();
        // TODO: Reply false if log doesn't contain an entry at prevLogIndex whose term matches pervLogTerm
        boolean success = requestTerm >= currentTerm;
        long term = success ? requestTerm : currentTerm;
        logger.debug("append entry success: {}, current term: {}, request term: {}",
                success, currentTerm, requestTerm);
        // TODO: update entry
        return AppendEntryResponse.newBuilder().setTerm(term)
                .setSuccess(success).build();
    }
}

