package jraft.impl;

import com.google.common.base.Preconditions;
import jraft.RaftServer;
import jraft.RaftServerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by Chen on 7/29/17.
 */
public class RaftServerImpl implements RaftServer {

    private final Logger logger = LoggerFactory.getLogger(RaftServerImpl.class);

    private boolean started;

    private final RaftServerContext context;

    public RaftServerImpl(RaftServerContext serverContext) {
        this.context = Preconditions.checkNotNull(serverContext);
    }

    @Override
    public void bootstrap(String selfConnectionString, List<String>
            peerConnectionStrings) {
        if (started) {
            logger.warn("server {} already started!", context.getServerName());
            return;
        }
        context.bootstrap(selfConnectionString, peerConnectionStrings);
        started = true;
    }

    @Override
    public void shutdown() {
        if (!started) {
            logger.warn("server {} is not running!", context.getServerName());
        }
        context.close();
        started = false;
    }
}
