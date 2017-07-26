package jraft.storage.metastore;

import com.google.common.base.Preconditions;

/**
 * Created by Chen on 7/23/17.
 */
public class MemoryBackedMetaStoreImpl implements MetaStore {

    private long term = 0;

    String memberId = "";

    @Override
    public synchronized void storeTerm(long term) {
        Preconditions.checkArgument(term >= 0);
        this.term = term;
    }

    @Override
    public synchronized long loadTerm() {
        return this.term;
    }


    @Override
    public synchronized void storeLastVote(String memberId) {
        Preconditions.checkArgument(memberId != null);
        this.memberId = memberId;
    }

    @Override
    public synchronized String loadLastVote() {
        return this.memberId;
    }


    @Override
    public void close() throws Exception {
    }
}
