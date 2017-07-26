package jraft.storage.metastore;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Provide functionality to store/load meta data to/from disk. The class is
 * optimized by leveraging {@link MappedByteBuffer} that mapping disk file to
 * a chunk of memory for fast read/write operations. Most user calls of
 * store/load to this class is in-memory. Underlying OS would take care of IO
 * with disk.
 * <p>
 * The serialized bytes format is:
 * | term(long) | string_len(int) | serverId({@link String})|
 * | 8 bytes    | 4 bytes         | of length string_len    |
 * <p>
 * Created by Chen on 7/23/17.
 */

@ThreadSafe
public class FileBackedMetaStoreImpl implements MetaStore {

    // default buffer size 256B
    private static final int BUFFER_SIZE = 256;

    // offset to read string length
    private static final int STRING_LEN_OFFSET = 8;

    // offset of beginning of serverId
    private static final int SERVER_ID_OFFSET = 12;

    private final Logger logger
            = LoggerFactory.getLogger(FileBackedMetaStoreImpl.class);

    private final RandomAccessFile dataFile;

    private final MappedByteBuffer mappedByteBuffer;


    public FileBackedMetaStoreImpl(String path) throws IOException {
        Preconditions.checkNotNull(path);
        Path dataFilePath = Paths.get(path);
        if (!Files.exists(dataFilePath, LinkOption.NOFOLLOW_LINKS)) {
            dataFilePath = Files.createFile(dataFilePath);
            logger.info("File {} not existed, creating now...", dataFilePath);
        }
        this.dataFile = new RandomAccessFile(dataFilePath.toFile(), "rw");
        this.mappedByteBuffer = dataFile.getChannel().map(FileChannel.MapMode
                .READ_WRITE, 0, BUFFER_SIZE);
    }

    @Override
    public synchronized void storeTerm(long term) {
        Preconditions.checkArgument(term >= 0);
        mappedByteBuffer.putLong(0, term);
    }

    @Override
    public synchronized long loadTerm() {
        return mappedByteBuffer.getLong(0);
    }

    @Override
    public synchronized void storeLastVote(String memberId) {
        Preconditions.checkArgument(memberId != null
                && memberId.length() <= BUFFER_SIZE - SERVER_ID_OFFSET);
        byte[] bytes = memberId.getBytes();
        // first encode len of serverId string
        mappedByteBuffer.putInt(STRING_LEN_OFFSET, bytes.length);
        // then encode the serverId string itself
        mappedByteBuffer.position(SERVER_ID_OFFSET);
        mappedByteBuffer.put(bytes);
    }

    @Override
    public synchronized String loadLastVote() {
        int serverIdLen = getServerIdStringLength();
        Preconditions.checkState(serverIdLen >= 0
                && serverIdLen <= BUFFER_SIZE - SERVER_ID_OFFSET);
        if (serverIdLen == 0) {
            logger.warn("no last voted server has been set yet!");
            return "";
        }
        byte[] serverIdStringBuffer = new byte[serverIdLen];
        mappedByteBuffer.position(SERVER_ID_OFFSET);
        mappedByteBuffer.get(serverIdStringBuffer);
        String lastVotedFor = new String(serverIdStringBuffer);
        return lastVotedFor;
    }

    @Override
    public void close() throws Exception {
        mappedByteBuffer.force();
    }

    private int getServerIdStringLength() {
        return mappedByteBuffer.getInt(STRING_LEN_OFFSET);
    }

}
