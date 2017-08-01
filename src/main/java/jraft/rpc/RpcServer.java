package jraft.rpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * A gRPC server that listens at {@link #port}
 * Created by Chen on 7/20/17.
 */
final public class RpcServer {

    private static final org.slf4j.Logger logger
            = LoggerFactory.getLogger(RpcServer.class.getName());

    private static final int DEFAULT_SERVER_PORT = 50051;

    private final int port;

    private Server server;

    public RpcServer(int port) {
        this.port = port;
    }

    public RpcServer() {
        this(DEFAULT_SERVER_PORT);
    }

    public void start() throws IOException {
        server = ServerBuilder.forPort(port)
                .addService(new RaftRpcImpl())
                .build().start();
        logger.info("RPC server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                RpcServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    public void stop() {
        if (server != null) {
            logger.info("shutting down gRPC server {}!", server);
            server.shutdown();
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
}
