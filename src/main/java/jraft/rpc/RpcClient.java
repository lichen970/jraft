package jraft.rpc;

import com.google.common.base.Preconditions;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import jraft.proto.*;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by Chen on 7/20/17.
 */
public final class RpcClient {

    private static final org.slf4j.Logger logger
            = LoggerFactory.getLogger(RpcClient.class.getName());

    private final ManagedChannel channel;

    private final RaftGrpc.RaftBlockingStub blockingStub;

    /**
     * Construct client connecting to {@link RpcServer} at {@code host:port}.
     */
    public RpcClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port)
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext(true));
    }

    /**
     * Construct client for accessing {@link RpcServer} server using the existing
     * channel.
     */
    RpcClient(ManagedChannelBuilder<?> channelBuilder) {
        channel = channelBuilder.build();
        blockingStub = RaftGrpc.newBlockingStub(channel);
    }

    /**
     * shut down the client
     *
     * @throws InterruptedException
     */
    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public VoteResponse sendVoteRequest(VoteRequest request) {
        Preconditions.checkArgument(request != null);
        logger.info("send vote request: {}", request);
        VoteResponse response = null;
        try {
            response = blockingStub.requestVote(request);
        } catch (StatusRuntimeException e) {
            logger.error("RPC failed: {}", e.getStatus());
        }
        return response;
    }

    public AppendEntryResponse sendAppendRequest(AppendEntryRequest request) {
        Preconditions.checkArgument(request != null);
        AppendEntryResponse response = null;
        try {
            response = blockingStub.append(request);
        } catch (StatusRuntimeException e) {
            logger.error("RPC failed: {}", e.getStatus());
        }
        return response;
    }
}
