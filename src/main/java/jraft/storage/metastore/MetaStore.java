package jraft.storage.metastore;

/**
 * load/store term and last voted member id to disk
 * Created by Chen on 7/23/17.
 */
public interface MetaStore extends AutoCloseable {

    void storeTerm(long term);

    long loadTerm();

    void storeLastVote(String serverId);

    String loadLastVote();
}
