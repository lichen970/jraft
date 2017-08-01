package jraft.impl;

import jraft.RaftServerContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Created by Chen on 7/31/17.
 */
public class RaftServerContextImplTest {
    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testBootstrap() throws Exception {
        RaftServerContext context1 = new RaftServerContextImpl
                ("localhost:50050", Executors.newScheduledThreadPool(1));
        RaftServerContext context2 = new RaftServerContextImpl
                ("localhost:50051", Executors.newScheduledThreadPool(1));

        List<String> l1 = new LinkedList<>();
        l1.add("192.168.0.50:50051");
        context1.bootstrap("localhost:50050", l1);
        /*
        List<String> l2 = new LinkedList<>();
        l2.add("localhost:50050");
        context2.bootstrap("localhost:50051", l2);
        */
    }

}