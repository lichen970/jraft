package server;


import org.junit.After;
import org.junit.Before;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Created by lche on 6/20/17.
 */
@RunWith(JUnit4.class)
public class NodeTest {

    protected Logger logger = LoggerFactory.getLogger(NodeTest.class);
    Node node=new Node();

    @Before
    public void setUp() {
        logger.debug("hello {}",1);

    }

    @After
    public void tearDown() {
        logger.warn("bye-bye {}",2);
    }

    @Test
    public void haha() throws Exception {
        node.haha();
    }

}