package io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component;

import static java.lang.Boolean.TRUE;
import static java.net.StandardProtocolFamily.UNIX;
import static org.springframework.boot.health.contributor.Status.UP;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;

/**
 * Lightweight Unix domain socket health server.
 *
 * <p>This component exposes application health over a Unix domain socket by returning a single-byte
 * response: {@code H} for healthy and {@code U} for unhealthy. The value is derived from Spring
 * Boot Actuator's {@link HealthEndpoint}.
 *
 * <p>The design targets containerized deployments where a minimal health check mechanism is
 * preferred over HTTP. In hardened images, tools such as {@code curl} or {@code wget} may be
 * unavailable, while a tiny socket client can still perform reliable health checks with minimal
 * overhead.
 *
 * <p>See the {@code docker/healthcheck.go} file for a minimal Go client implementation that can be
 * used to probe the health socket server.
 */
@Slf4j
public class HealthSocketServer implements AutoCloseable {

  public static final String DEFAULT_THREAD_NAME = "health-socket-server";

  /** Response byte representing a healthy application state. */
  private static final char HEALTHY = 'H';

  /** Response byte representing an unhealthy application state. */
  private static final char UNHEALTHY = 'U';

  /**
   * Timeout duration for shutting down the server thread gracefully.
   *
   * <p>If the server thread does not terminate within this duration after being interrupted, it
   * will be forcefully terminated. This ensures that the application can shut down cleanly without
   * hanging indefinitely due to a stuck server thread.
   */
  private static final Duration SERVER_THREAD_SHUTDOWN_TIMEOUT = Duration.ofMillis(3000);

  private static final Duration WAIT_UNTIL_TIMEOUT = Duration.ofMillis(5000);

  /**
   * Maximum number of consecutive failed write attempts to a client socket before the server
   * considers the connection unhealthy and logs a warning.
   *
   * <p>This threshold helps to avoid flooding the logs with repeated write failures while still
   * alerting to potential issues.
   */
  private static final int MAX_FAILED_WRITE_COUNT = 5;

  /** Actuator endpoint used to obtain the current application health status. */
  private final HealthEndpoint healthEndpoint;

  /** Filesystem path of the Unix domain socket. */
  private final Path socketPath;

  /**
   * Runtime abstraction to facilitate testing and allow for different implementations of virtual
   * thread creation and socket channel management.
   */
  private final Runtime runtime;

  /**
   * Reference to the thread running the health socket server, allowing for controlled shutdown and
   * resource cleanup.
   */
  private final AtomicReference<Thread> serverThreadReference;

  private final AtomicReference<Boolean> socketBound = new AtomicReference<>(false);

  /**
   * Constructs a new HealthSocketServer with the specified health endpoint and socket path.
   *
   * @param socketPath the filesystem path of the Unix domain socket
   * @param healthEndpoint the health endpoint used to obtain the current application health status
   * @param virtualThreadBuilder the virtual thread builder used to create the server thread
   */
  @Builder
  public HealthSocketServer(
      Path socketPath,
      HealthEndpoint healthEndpoint,
      Thread.Builder.OfVirtual virtualThreadBuilder) {
    this(socketPath, healthEndpoint, new DefaultRuntime(virtualThreadBuilder));
  }

  /**
   * Package-private constructor for testing purposes, allowing injection of a custom runtime.
   *
   * @param socketPath the filesystem path of the Unix domain socket
   * @param healthEndpoint the health endpoint used to obtain the current application health status
   * @param runtime the runtime abstraction for virtual thread creation and socket channel
   *     management
   */
  HealthSocketServer(Path socketPath, HealthEndpoint healthEndpoint, Runtime runtime) {
    this(socketPath, healthEndpoint, runtime, new AtomicReference<>(null));
  }

  /**
   * Package-private constructor for testing purposes, allowing injection of a custom runtime and
   * server thread reference.
   *
   * @param socketPath the filesystem path of the Unix domain socket
   * @param healthEndpoint the health endpoint used to obtain the current application health status
   * @param runtime the runtime abstraction for virtual thread creation and socket channel
   *     management
   * @param serverThreadReference the atomic reference to the thread running the health socket
   *     server
   */
  HealthSocketServer(
      Path socketPath,
      HealthEndpoint healthEndpoint,
      Runtime runtime,
      AtomicReference<Thread> serverThreadReference) {
    this.healthEndpoint = healthEndpoint;
    this.socketPath = socketPath;
    this.runtime = runtime;
    this.serverThreadReference = serverThreadReference;
  }

  /**
   * Starts the health socket server in a virtual thread.
   *
   * <p>The server listens for incoming connections on the specified Unix domain socket and responds
   * with the current health status.
   */
  @PostConstruct
  public HealthSocketServer start() {
    var existingThread = serverThreadReference.get();

    if (existingThread != null) {
      log.warn("Health socket server is already running on thread: {}", existingThread.getName());
      return this;
    }

    var newServerThread = this.runtime.serverThreadFactory().newThread(new ServerProcess());
    if (serverThreadReference.compareAndSet(null, newServerThread)) {
      newServerThread.start();
    } else {
      log.warn(
          "Health socket server thread was already set: {}", serverThreadReference.get().getName());
    }

    return this;
  }

  public Optional<String> serverThreadName() {
    var thread = serverThreadReference.get();
    return thread != null ? Optional.of(thread.getName()) : Optional.empty();
  }

  public HealthSocketServer interrupt() {
    var thread = serverThreadReference.get();
    if (thread != null) {
      thread.interrupt();
    }
    return this;
  }

  /**
   * Stops the health socket server thread and cleans up the Unix domain socket file.
   *
   * <p>This method interrupts the server thread and waits for it to terminate gracefully. If the
   * thread does not terminate within the specified timeout, a warning is logged.
   */
  @Override
  public void close() {
    stopServerThread();
    cleanUpSocketFile();
  }

  /**
   * Returns the current status of the health socket server.
   *
   * @return the current status
   */
  public Status status() {
    var serverThread = serverThreadReference.get();
    if (serverThread == null) {
      return Status.STOPPED;
    } else if (serverThread.isAlive() && !serverThread.isInterrupted()) {
      if (TRUE.equals(socketBound.get())) {
        return Status.RUNNING;
      } else {
        return Status.STARTING;
      }
    } else {
      return Status.STOPPING;
    }
  }

  public HealthSocketServer waitUntil(Status status) {
    return waitUntil(status, WAIT_UNTIL_TIMEOUT);
  }

  public HealthSocketServer waitUntil(Status status, Duration timeout) {
    long deadlineNanos = System.nanoTime() + timeout.toNanos();
    while (System.nanoTime() < deadlineNanos && this.status() != status) {
      Thread.yield();
    }
    if (this.status() != status) {
      log.warn(
          "HealthSocketServer did not reach status {} within {} ms, current status: {}",
          status,
          timeout.toMillis(),
          this.status());
      throw new ServerStatusTimeoutException(
          "HealthSocketServer did not reach status "
              + status
              + " within "
              + timeout.toMillis()
              + " ms");
    }
    return this;
  }

  /**
   * Returns the filesystem path of the Unix domain socket used by the health socket server.
   *
   * @return the socket path
   */
  public Path socketPath() {
    return socketPath;
  }

  /**
   * Stops the running server thread, if present, using cooperative interruption.
   *
   * <p>The method clears the shared thread reference first to avoid races with subsequent {@link
   * #start()} calls. If a live thread exists, it is interrupted and the method waits up to {@link
   * #SERVER_THREAD_SHUTDOWN_TIMEOUT} for termination.
   */
  private void stopServerThread() {
    var serverThread = serverThreadReference.getAndSet(null);

    if (serverThread != null && serverThread.isAlive()) {
      log.info("Stopping HealthSocketServer thread: {}", serverThread.getName());
      serverThread.interrupt();

      try {
        serverThread.join(SERVER_THREAD_SHUTDOWN_TIMEOUT);

        if (serverThread.isAlive()) {
          log.warn(
              "HealthSocketServer thread failed to terminate within {} ms timeout",
              SERVER_THREAD_SHUTDOWN_TIMEOUT);
        } else {
          log.info("HealthSocketServer thread terminated successfully");
        }
      } catch (InterruptedException e) {
        log.warn(
            "Interrupted while waiting for HealthSocketServer thread to terminate: {}",
            e.getMessage());
        serverThread.interrupt();
      }
    }
  }

  /**
   * Removes the Unix domain socket file from the filesystem, if it exists.
   *
   * <p>Cleanup failures are intentionally non-fatal and only logged because stale socket files do
   * not affect the current JVM process once shutdown is in progress.
   */
  private void cleanUpSocketFile() {
    try {
      runtime.deleteIfExists(socketPath);
      log.debug("Successfully cleaned up health socket file at: {}", socketPath);
    } catch (IOException e) {
      log.warn("Failed to clean up socket file during shutdown: {}", e.getMessage());
    }
  }

  /** Possible states of the health socket server. */
  public enum Status {
    STOPPED,
    STARTING,
    RUNNING,
    STOPPING
  }

  /**
   * Abstraction layer for thread and socket operations used by {@link HealthSocketServer}.
   *
   * <p>This indirection keeps production code simple while allowing deterministic tests to inject
   * fakes for thread creation and filesystem/network interactions.
   */
  interface Runtime {
    /**
     * Returns the thread factory used to create the server thread.
     *
     * @return thread factory for server execution
     */
    ThreadFactory serverThreadFactory();

    /**
     * Opens a server socket channel for Unix domain socket communication.
     *
     * @return an opened, unbound server socket channel
     * @throws IOException if channel creation fails
     */
    ServerSocketChannel serverSocketChannel() throws IOException;

    /**
     * Deletes a file if it exists.
     *
     * @param path path to delete
     * @throws IOException if deletion fails
     */
    void deleteIfExists(Path path) throws IOException;
  }

  /** Default production {@link Runtime} implementation backed by JDK virtual threads and NIO. */
  static class DefaultRuntime implements Runtime {

    private final ThreadFactory threadFactory;

    /**
     * Creates a runtime using the supplied virtual thread builder.
     *
     * @param virtualThreadBuilder virtual thread builder to derive a thread factory from
     */
    DefaultRuntime(Thread.Builder.OfVirtual virtualThreadBuilder) {
      if (virtualThreadBuilder == null) {
        virtualThreadBuilder = Thread.ofVirtual().name(DEFAULT_THREAD_NAME);
      }
      this.threadFactory = virtualThreadBuilder.factory();
    }

    /** {@inheritDoc} */
    @Override
    public ThreadFactory serverThreadFactory() {
      return threadFactory;
    }

    /** {@inheritDoc} */
    @Override
    public ServerSocketChannel serverSocketChannel() throws IOException {
      return ServerSocketChannel.open(UNIX);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteIfExists(Path path) throws IOException {
      java.nio.file.Files.deleteIfExists(path);
    }
  }

  public static class ServerStatusTimeoutException extends RuntimeException {
    public ServerStatusTimeoutException(String message) {
      super(message);
    }
  }

  /**
   * Accept loop runnable for the Unix domain socket server.
   *
   * <p>Each accepted client receives exactly one byte representing the current health state.
   */
  class ServerProcess implements Runnable {

    private final AtomicInteger failedWriteCount = new AtomicInteger(0);

    /**
     * Runs the blocking accept loop until interrupted or an unrecoverable I/O failure occurs.
     *
     * <p>Socket file cleanup is always attempted in a {@code finally} block.
     */
    @Override
    public void run() {
      var thread = Thread.currentThread();

      try (var serverSocket = runtime.serverSocketChannel()) {
        var address = UnixDomainSocketAddress.of(socketPath);
        serverSocket.bind(address);
        socketBound.set(true);
        log.info("Health socket server listening on: {}", socketPath);
        while (!thread.isInterrupted()) {
          listenOn(serverSocket);
        }
      } catch (ClosedByInterruptException _) {
        log.info("Health socket server thread was interrupted, shutting down gracefully");
      } catch (IOException e) {
        log.error("IOException occurred", e);
        thread.interrupt();
      } finally {
        socketBound.set(false);
        close();
      }
    }

    /**
     * Accepts a single client connection and processes it.
     *
     * @param serverSocket the server socket channel to accept connections from
     * @throws IOException if an I/O error occurs
     */
    private void listenOn(ServerSocketChannel serverSocket) throws IOException {
      try (var clientSocket = serverSocket.accept()) {
        log.debug("Accepted connection from client: {}", clientSocket.getRemoteAddress());
        processConnection(clientSocket);
      }
    }

    /**
     * Handles a single client socket connection.
     *
     * <p>The method maps the current actuator health to one response byte: {@code H} for {@code
     * UP}, otherwise {@code U}. Any exception while reading health or writing to the socket results
     * in an unhealthy response or warning log.
     *
     * @param socket accepted client socket
     */
    void processConnection(SocketChannel socket) {
      char response;

      try {
        var health = healthEndpoint.health();
        var healthStatus = health.getStatus();
        response = healthStatus.equals(UP) ? HEALTHY : UNHEALTHY;
      } catch (Exception e) {
        log.warn("Exception occurred while checking health status, responding with UNHEALTHY", e);
        response = UNHEALTHY;
      }

      try {
        socket.write(ByteBuffer.wrap(new byte[] {(byte) response}));
        log.debug("Health response sent to client: {}", response);
        failedWriteCount.set(0);
      } catch (IOException e) {
        var currentFailedCount = failedWriteCount.incrementAndGet();
        if (currentFailedCount >= MAX_FAILED_WRITE_COUNT) {
          log.warn(
              "Failed to write health response to client (consecutive failures: {})",
              currentFailedCount,
              e);
        } else {
          log.debug(
              "Failed to write health response to client (consecutive failures: {})",
              currentFailedCount,
              e);
        }
      }
    }
  }
}
