package jraft.rpc;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import jraft.proto.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Chen on 7/20/17.
 */
@RunWith(JUnit4.class)
public class gRpcServerTest {

    private static final String UNIQUE_SERVER_NAME =
            "in-process server for " + gRpcServerTest.class;

    private final Server inProcessServer = InProcessServerBuilder
            .forName(UNIQUE_SERVER_NAME).addService(new RaftRpcImpl()).directExecutor()
            .build();

    private final ManagedChannel inProcessChannel =
            InProcessChannelBuilder.forName(UNIQUE_SERVER_NAME).directExecutor().build();

    /**
     * Creates and starts the server with the {@link InProcessServerBuilder},
     * and creates an in-process channel with the {@link InProcessChannelBuilder}.
     */
    @Before
    public void setUp() throws Exception {
        inProcessServer.start();
    }

    /**
     * Shuts down the in-process channel and server.
     */
    @After
    public void tearDown() {
        inProcessChannel.shutdownNow();
        inProcessServer.shutdownNow();
    }

    @Test
    public void testAppendEntry() throws Exception {
        String leaderId = "LEADER_0";
        long term = 55;
        RaftGrpc.RaftBlockingStub blockingStub = RaftGrpc.newBlockingStub(inProcessChannel);
        AppendEntryResponse response = blockingStub.append(AppendEntryRequest
                .newBuilder().setLeaderId(leaderId).setTerm(term).build());
        assertEquals(term, response.getTerm());
        assertTrue(response.getSuccess());
    }

    @Test
    public void testVote() throws Exception {
        String candidateId = "candidate_0";
        long term = 55;
        RaftGrpc.RaftBlockingStub blockingStub = RaftGrpc.newBlockingStub(inProcessChannel);
        VoteResponse response = blockingStub.requestVote(VoteRequest
                .newBuilder().setCandidateId(candidateId).setTerm(term).build());
        assertEquals(term, response.getTerm());
        assertTrue(response.getVoteGranted());
    }
}