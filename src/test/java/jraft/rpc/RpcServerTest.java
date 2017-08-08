package jraft.rpc;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import jraft.RaftServer;
import jraft.impl.RaftServerContextImpl;
import jraft.proto.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Created by Chen on 7/20/17.
 */
@RunWith(JUnit4.class)
public class RpcServerTest {

    private static final String UNIQUE_SERVER_NAME =
            "in-process server for " + RpcServerTest.class;

    @Mock
    private static final RaftServerContextImpl raftServerContextImpl
            = mock(RaftServerContextImpl.class);

    private final Server inProcessServer = InProcessServerBuilder
            .forName(UNIQUE_SERVER_NAME).addService(new RaftRpcImpl(raftServerContextImpl)).directExecutor()
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
        when(raftServerContextImpl.getTerm()).thenReturn(term).thenReturn(term+1);
        RaftGrpc.RaftBlockingStub blockingStub = RaftGrpc.newBlockingStub(inProcessChannel);
        AppendEntryResponse response = blockingStub.append(AppendEntryRequest
                .newBuilder().setLeaderId(leaderId).setTerm(term).build());
        assertEquals(term, response.getTerm());
        assertTrue(response.getSuccess());

        response = blockingStub.append(AppendEntryRequest
                .newBuilder().setLeaderId(leaderId).setTerm(term).build());
        assertEquals(term+1, response.getTerm());
        assertFalse(response.getSuccess());
    }

    @Test
    public void testVote() throws Exception {
        String candidateId = "candidate_0";
        String candidateIdVoted = "candidate_1";
        long term = 55;
        when(raftServerContextImpl.getTerm()).thenReturn(term).thenReturn(term).thenReturn(term+1).thenReturn(term+1);
        when(raftServerContextImpl.getLastVoteFor())
                .thenReturn(null).thenReturn(candidateIdVoted).thenReturn(null).thenReturn(candidateIdVoted);
        RaftGrpc.RaftBlockingStub blockingStub = RaftGrpc.newBlockingStub(inProcessChannel);
        VoteResponse response = blockingStub.requestVote(VoteRequest
                .newBuilder().setCandidateId(candidateId).setTerm(term).build());
        assertEquals(term, response.getTerm());
        assertTrue(response.getVoteGranted());

        response = blockingStub.requestVote(VoteRequest
                .newBuilder().setCandidateId(candidateId).setTerm(term).build());
        assertEquals(term, response.getTerm());
        assertFalse(response.getVoteGranted());

        response = blockingStub.requestVote(VoteRequest
                .newBuilder().setCandidateId(candidateId).setTerm(term).build());
        assertEquals(term+1, response.getTerm());
        assertFalse(response.getVoteGranted());

        response = blockingStub.requestVote(VoteRequest
                .newBuilder().setCandidateId(candidateId).setTerm(term).build());
        assertEquals(term+1, response.getTerm());
        assertFalse(response.getVoteGranted());
    }
}