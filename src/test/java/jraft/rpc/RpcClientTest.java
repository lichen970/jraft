package jraft.rpc;

import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import jraft.RaftServerContext;
import jraft.impl.RaftServerContextImpl;
import jraft.proto.AppendEntryRequest;
import jraft.proto.RaftGrpc;
import jraft.proto.VoteRequest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Created by Chen on 7/20/17.
 */
@RunWith(JUnit4.class)
public class RpcClientTest {

    private final RaftGrpc.RaftImplBase serviceImpl = spy(new RaftRpcImpl(mock(RaftServerContextImpl.class)));

    private Server fakeServer;
    private RpcClient client;

    /**
     * Creates and starts a fake in-process server, and creates a client with an in-process channel.
     */
    @Before
    public void setUp() throws Exception {
        String uniqueServerName = "fake server for " + getClass();
        fakeServer = InProcessServerBuilder
                .forName(uniqueServerName).directExecutor().addService(serviceImpl).build().start();
        InProcessChannelBuilder channelBuilder =
                InProcessChannelBuilder.forName(uniqueServerName).directExecutor();
        client = new RpcClient(channelBuilder);
    }

    /**
     * Shuts down the client and server.
     */
    @After
    public void tearDown() throws Exception {
        client.shutdown();
        fakeServer.shutdownNow();
    }

    @Test
    public void testSendAppend() throws Exception {
        long term = 55;
        String leaderId = "leader_0";
        ArgumentCaptor<AppendEntryRequest> requestCaptor
                = ArgumentCaptor.forClass(AppendEntryRequest.class);
        AppendEntryRequest request
                = AppendEntryRequest.newBuilder().setTerm(term)
                .setLeaderId(leaderId).build();
        client.sendAppendRequest(request);
        verify(serviceImpl).append(requestCaptor.capture(), any());
        Assert.assertSame(request, requestCaptor.getValue());
    }

    @Test
    public void testSendRequestVote() throws Exception {
        long term = 55;
        String candidateId = "candidate_0";
        ArgumentCaptor<VoteRequest> requestCaptor
                = ArgumentCaptor.forClass(VoteRequest.class);
        VoteRequest request
                = VoteRequest.newBuilder().setTerm(term)
                .setCandidateId(candidateId).build();
        client.sendVoteRequest(request);
        verify(serviceImpl).requestVote(requestCaptor.capture(), any());
        Assert.assertSame(request, requestCaptor.getValue());
    }
}