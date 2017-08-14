package jraft.impl;

import jraft.RaftServerContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Created by Chen on 7/31/17.
 */
@RunWith(JUnit4.class)
public class RaftServerContextImplTest {
    static RaftServerContextImpl raftServerContext;
    static final String serverName1 = "server1";
    static final String serverName2 = "server2";
    static final String serverIpAndPort = "localhost:50050";

    @Before
    public void setUp() throws Exception {
        raftServerContext = new RaftServerContextImpl(
                serverName1, Executors.newScheduledThreadPool(1));
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetServerName() throws Exception {
        Assert.assertEquals(raftServerContext.getServerName(), serverName1);
    }

    @Test
    public void testTermOperations() throws Exception {
        Assert.assertEquals(raftServerContext.getTerm(), 0);
        long term = ThreadLocalRandom.current().nextLong(0, 10000);
        raftServerContext.setTerm(term);
        Assert.assertEquals(raftServerContext.getTerm(), term);
    }

    @Test
    public void testLastVoteForOperations() throws Exception {
        Assert.assertEquals(raftServerContext.getLastVoteFor(), null);
        raftServerContext.setLastVoteFor(serverName2);
        Assert.assertEquals(raftServerContext.getLastVoteFor(), serverName2);
    }

    @Test
    public void testLeaderIdOperations() throws Exception {
        Assert.assertEquals(raftServerContext.getLeaderId(), null);
        raftServerContext.setLeaderId(serverName2);
        Assert.assertEquals(raftServerContext.getLeaderId(), serverName2);
    }

    @Test
    public void testPeersOperations() throws Exception {
        Assert.assertEquals(raftServerContext.getPeers().size(), 0);
        raftServerContext.addMemberToPeers(serverName2, serverIpAndPort);
        Assert.assertEquals(raftServerContext.getPeers().size(), 1);
        raftServerContext.removeMemberFromPeers(serverName2);
        Assert.assertEquals(raftServerContext.getPeers().size(), 0);
    }

    @Test
    public void testSingleServerContextBootstrap() throws Exception {
        raftServerContext.bootstrap(serverIpAndPort, new LinkedList<String>());
        Assert.assertEquals(raftServerContext.getCurrentRole(), RaftServerContext.Role.FOLLOWER);
    }

    @Test
    public void testPairServerContextBootstrap() throws Exception {
        RaftServerContext context1 = new RaftServerContextImpl
                (serverName1, Executors.newScheduledThreadPool(1));
        RaftServerContext context2 = new RaftServerContextImpl
                (serverName2, Executors.newScheduledThreadPool(1));

        List<String> l1 = new LinkedList<>();
        l1.add("localhost:50052");
        context1.bootstrap("localhost:50051", l1);

        List<String> l2 = new LinkedList<>();
        l2.add("localhost:50051");
        context2.bootstrap("localhost:50052", l2);
        context1.getScheduler().awaitTermination(30, TimeUnit.SECONDS);
    }

}