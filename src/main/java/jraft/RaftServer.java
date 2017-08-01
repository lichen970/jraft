package jraft;

import java.util.List;

/**
 * Created by Chen on 7/20/17.
 */
public interface RaftServer {

    /**
     * start the server.
     *
     * @param selfConnectionString  my ip:port
     * @param peerConnectionStrings peer ip:port
     */
    void bootstrap(String selfConnectionString, List<String>
            peerConnectionStrings);

    void shutdown();
}
