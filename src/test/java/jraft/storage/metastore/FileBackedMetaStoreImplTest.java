package jraft.storage.metastore;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.*;

/**
 * Created by Chen on 7/24/17.
 */
@RunWith(JUnit4.class)
public class FileBackedMetaStoreImplTest {

    static Path dataPath;

    @BeforeClass
    public static void setUpClass() throws Exception {
        dataPath = Paths.get(".").toAbsolutePath()
                .normalize().resolve("data/testData");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {

    }

    @Test
    public void testCleanBootup() throws Exception {
        // mimic first time boot with clean start
        Files.deleteIfExists(dataPath);
        MetaStore ms = new FileBackedMetaStoreImpl(dataPath.toString());
        Assert.assertEquals("", ms.loadLastVote());
        Assert.assertEquals(0, ms.loadTerm());
    }

    @Test
    public void testTerm() throws Exception {
        MetaStore ms = new FileBackedMetaStoreImpl(dataPath.toString());
        long term = ThreadLocalRandom.current().nextLong(0, 10000);
        ms.storeTerm(term);
        Assert.assertEquals(term, ms.loadTerm());
        term = ThreadLocalRandom.current().nextLong(0, 10000);
        ms.storeTerm(term);
        Assert.assertEquals(term, ms.loadTerm());
    }

    @Test
    public void testServerId() throws Exception {
        Files.deleteIfExists(dataPath);
        MetaStore ms = new FileBackedMetaStoreImpl(dataPath.toString());
        String id = "192.168.5.64:9999";
        ms.storeLastVote(id);
        Assert.assertEquals(id, ms.loadLastVote());
    }

    @Test
    public void testMultiOperationsMultiThreads() throws Exception {
        ExecutorService scheduler = Executors.newFixedThreadPool(5);
        Collection<Callable<Void>> toDo = new HashSet();
        createTasks(toDo);
        scheduler.invokeAll(toDo);
        scheduler.shutdown();
        scheduler.awaitTermination(5, TimeUnit.SECONDS);
    }

    private void createTasks(Collection<Callable<Void>> toDo) throws
            IOException {
        MetaStore ms = new FileBackedMetaStoreImpl(dataPath.toString());

        for (int i = 0; i < 5; i++) {
            toDo.add(() -> {
                long term = ms.loadTerm();
                System.out.println("Load term: " + (term + 1));
                ms.storeTerm(ThreadLocalRandom.current().nextLong(0, 1000));
                return null;
            });
        }
        for (int i = 0; i < 5; i++) {
            toDo.add(() -> {
                System.out.println("Load last vote: " + ms.loadLastVote());
                ms.storeLastVote(Integer.toString(
                        ThreadLocalRandom.current().nextInt())
                );
                return null;
            });
        }
    }
}