package jraft;

import java.util.Collection;

/**
 * Created by Chen on 7/20/17.
 */
public interface RaftServer {

    void bootstrap(Collection<String> serverNames);

    void shutdown();
}
