package jraft.rpc;

import io.grpc.stub.StreamObserver;
import jraft.proto.*;

/**
 * Created by Chen on 7/20/17.
 */
public class RaftRpcImpl extends RaftGrpc.RaftImplBase {

    @Override
    public void requestVote(VoteRequest request,
                            StreamObserver<VoteResponse> responseObserver) {
        VoteResponse response = getVoteResponse(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private VoteResponse getVoteResponse(VoteRequest request) {
        return VoteResponse.newBuilder().setTerm(request.getTerm())
                .setVoteGranted(true).build();
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
        return AppendEntryResponse.newBuilder().setTerm(request.getTerm())
                .setSuccess(true).build();
    }
}

