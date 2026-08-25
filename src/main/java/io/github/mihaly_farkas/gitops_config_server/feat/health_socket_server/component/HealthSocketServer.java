package io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component;

import static io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component.HealthSocketServer.ApplicationStatus.HEALTHY;
import static io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component.HealthSocketServer.ApplicationStatus.UNHEALTHY;
import static java.net.StandardProtocolFamily.UNIX;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Objects.isNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.springframework.boot.health.contributor.Status.UP;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;

/**
 * Lightweight Unix domain socket health server.
 *
 * <p>This component exposes application health over a Unix domain socket by returning a single-byte
 * response: {@code H} for healthy and {@code U} for unhealthy. This value is backed by Spring Boot
 * Actuator's {@link HealthEndpoint}.
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

  /**
   * Default name of the worker thread.
   *
   * <p>If no thread builder is provided via the {@link
   * HealthSocketServerBuilder#virtualThreadBuilder} method, the server creates a default builder
   * and uses this name for the created threads.
   */
  public static final String DEFAULT_THREAD_NAME = "health-socket-server";

  /** Log message for exceptions. */
  private static final String LOG_MESSAGE_EXCEPTION_OCCURRED = "Exception occurred";

  /** Default timeout used by {@link #close()}. */
  private static final Duration DEFAULT_SERVER_THREAD_SHUTDOWN_TIMEOUT = Duration.ofMillis(3000);

  /** Default timeout used by {@link #waitUntil(Status)}. */
  private static final Duration DEFAULT_WAIT_UNTIL_TIMEOUT = Duration.ofMillis(5000);

  /**
   * Maximum number of consecutive failed write attempts to a client socket before the server
   * considers the connection unhealthy and logs a warning.
   *
   * <p>This threshold helps to avoid flooding the logs with repeated write failures while still
   * flagging potential issues.
   */
  private static final int FAILED_WRITE_COUNT_THRESHOLD = 5;

  /** Filesystem path of the Unix domain socket. */
  @NotNull private final Path socketPath;

  /** Actuator endpoint used to obtain the current application health status. */
  @NotNull private final HealthEndpoint healthEndpoint;

  /**
   * Runtime abstraction to facilitate testing and allow for different implementations of virtual
   * thread creation and socket channel management.
   */
  @NotNull private final Runtime runtime;

  /**
   * Reference to the thread running the health socket server, allowing for controlled shutdown and
   * resource cleanup.
   */
  @NotNull private final AtomicReference<Thread> workerThreadReference;

  /**
   * Monitor object used by {@link #waitUntil(Status, Duration)} for status-change notifications.
   */
  @NotNull private final Object statusMonitor = new Object();

  /** Tracks whether the Unix domain socket is currently bound and accepting connections. */
  @NotNull private final AtomicBoolean socketReady = new AtomicBoolean(false);

  /**
   * Constructs a new HealthSocketServer with the specified health endpoint and socket path.
   *
   * @param socketPath the filesystem path of the Unix domain socket
   * @param healthEndpoint the health endpoint used to obtain the current application health status
   * @param virtualThreadBuilder the virtual thread builder used to create the worker thread
   */
  @Builder
  public HealthSocketServer(
      Path socketPath,
      HealthEndpoint healthEndpoint,
      Thread.Builder.OfVirtual virtualThreadBuilder) {
    if (isNull(socketPath)) {
      throw new IllegalArgumentException("socketPath must not be null");
    }
    if (isNull(healthEndpoint)) {
      throw new IllegalArgumentException("healthEndpoint must not be null");
    }

    this(
        socketPath,
        healthEndpoint,
        new DefaultRuntime(
            Objects.isNull(virtualThreadBuilder)
                ? Thread.ofVirtual().name(DEFAULT_THREAD_NAME)
                : virtualThreadBuilder),
        new AtomicReference<>(null));
  }

  /**
   * Package-private constructor, allowing injection of custom components.
   *
   * @param socketPath the filesystem path of the Unix domain socket
   * @param healthEndpoint the health endpoint used to obtain the current application health status
   * @param runtime the runtime abstraction for virtual thread creation and socket channel
   *     management
   * @param workerThreadReference the atomic reference to the thread running the health socket
   *     server
   */
  HealthSocketServer(
      Path socketPath,
      HealthEndpoint healthEndpoint,
      Runtime runtime,
      AtomicReference<Thread> workerThreadReference) {
    this.healthEndpoint = healthEndpoint;
    this.socketPath = socketPath;
    this.runtime = runtime;
    this.workerThreadReference = workerThreadReference;
  }

  /**
   * Starts the health socket server.
   *
   * <p>The server listens for incoming connections on the specified Unix socket and responds with
   * the current health status.
   */
  @PostConstruct
  public HealthSocketServer start() {
    var existingThread = workerThreadReference.get();

    if (existingThread != null) {
      log.warn("Already running, worker thread name: {}", existingThread.getName());
      return this;
    }

    var newWorkerThread = this.runtime.workerThreadFactory().newThread(new WorkerProcess());

    if (workerThreadReference.compareAndSet(null, newWorkerThread)) {
      signalStatusChange();
      newWorkerThread.start();
    } else {
      log.warn("Already running, worker thread name: {}", workerThreadReference.get().getName());
    }

    return this;
  }

  /**
   * Waits for the server to reach the specified status using a default timeout.
   *
   * <p>This method blocks the caller until the server reaches the expected status or the default
   * timeout ({@link #DEFAULT_WAIT_UNTIL_TIMEOUT}) expires.
   *
   * @param status the target status to wait for
   * @return this instance for method chaining
   * @throws ServerStatusTimeoutException if the target status is not reached within the timeout
   */
  public HealthSocketServer waitUntil(Status status) throws InterruptedException {
    return waitUntil(status, DEFAULT_WAIT_UNTIL_TIMEOUT);
  }

  /**
   * Waits for the server to reach the specified status with a custom timeout.
   *
   * <p>This method blocks the caller until the server reaches the expected status or the specified
   * timeout expires.
   *
   * @param expectedStatus the expected status to wait for
   * @param timeout the maximum duration to wait
   * @return this instance for method chaining
   * @throws ServerStatusTimeoutException if the expected status is not reached within the specified
   *     timeout
   */
  public HealthSocketServer waitUntil(Status expectedStatus, Duration timeout)
      throws InterruptedException {
    long deadlineNanos = System.nanoTime() + timeout.toNanos();

    synchronized (statusMonitor) {
      while (this.status() != expectedStatus) {
        long remainingNanos = deadlineNanos - System.nanoTime();
        if (remainingNanos <= 0) {
          break;
        }
        long waitMillis = NANOSECONDS.toMillis(remainingNanos);
        int waitNanos = (int) Math.max(0L, remainingNanos - MILLISECONDS.toNanos(waitMillis));
        statusMonitor.wait(waitMillis, waitNanos);
      }
    }

    if (this.status() != expectedStatus) {
      throw new ServerStatusTimeoutException(expectedStatus, timeout, this.status());
    }

    return this;
  }

  /**
   * Returns the filesystem path of the Unix socket.
   *
   * @return the socket path
   */
  public Path socketPath() {
    return socketPath;
  }

  /**
   * Returns the current status.
   *
   * @return the current status
   */
  public Status status() {
    var workerThread = workerThreadReference.get();
    if (workerThread == null) {
      return Status.STOPPED;
    } else if (workerThread.isAlive() && !workerThread.isInterrupted()) {
      if (socketReady.get()) {
        return Status.RUNNING;
      } else {
        return Status.STARTING;
      }
    } else {
      return Status.STOPPING;
    }
  }

  /**
   * Returns the name of the worker thread, if running.
   *
   * @return an {@link Optional} containing the thread name if the worker thread is running,
   *     otherwise empty
   */
  public Optional<String> workerThreadName() {
    var thread = workerThreadReference.get();
    return isNull(thread) ? Optional.empty() : Optional.of(thread.getName());
  }

  /** Stops the worker thread. */
  @Override
  public void close() {
    var workerThread = workerThreadReference.get();
    if (workerThread != null) {
      signalStatusChange();
      if (workerThread.isAlive()) {
        workerThread.interrupt();
        signalStatusChange();
        try {
          workerThread.join(DEFAULT_SERVER_THREAD_SHUTDOWN_TIMEOUT);
          if (workerThread.isAlive()) {
            log.warn(
                "Worker thread failed to terminate within {} milliseconds",
                DEFAULT_SERVER_THREAD_SHUTDOWN_TIMEOUT.toMillis());
          } else {
            workerThreadReference.compareAndSet(workerThread, null);
          }
        } catch (InterruptedException e) {
          workerThread.interrupt();
          log.debug(LOG_MESSAGE_EXCEPTION_OCCURRED, e);
        }
      } else {
        log.debug("Worker thread is already terminated: {}", workerThread.getName());
        workerThreadReference.compareAndSet(workerThread, null);
      }
    }
  }

  /** Wakes the threads waiting for a server status transition. */
  private void signalStatusChange() {
    synchronized (statusMonitor) {
      statusMonitor.notifyAll();
    }
  }

  /**
   * Represents the health status of the application.
   *
   * <p>Each status has a single-character representation used by the health socket protocol and a
   * human-readable textual representation.
   */
  public enum ApplicationStatus {

    /** The application is healthy and operating normally. */
    HEALTHY('H', "healthy"),

    /** The application is unhealthy and should be considered unavailable. */
    UNHEALTHY('U', "unhealthy");

    private final char statusChar;
    private final String statusText;

    ApplicationStatus(char statusChar, String statusText) {
      this.statusChar = statusChar;
      this.statusText = statusText;
    }

    /**
     * Returns the single-character representation of this status.
     *
     * @return the status character
     */
    public char statusChar() {
      return statusChar;
    }

    /**
     * Returns the human-readable textual representation of this status.
     *
     * @return the status text
     */
    public String statusText() {
      return statusText;
    }
  }

  /** Represents the lifecycle state of the health socket server. */
  public enum Status {

    /** The health socket server has been stopped and is not running. */
    STOPPED,

    /** The health socket server is starting and is not yet ready to serve requests. */
    STARTING,

    /** The health socket server is running and ready to serve requests. */
    RUNNING,

    /** The health socket server is shutting down and is no longer starting new work. */
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
     * Returns the thread factory used to create the long-running worker thread.
     *
     * @return thread factory for server execution
     */
    ThreadFactory workerThreadFactory();

    /**
     * Opens a server socket channel for Unix socket communication.
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
    boolean deleteIfExists(Path path) throws IOException;
  }

  /** Default production {@link Runtime} implementation backed by JDK virtual threads and NIO. */
  static class DefaultRuntime implements Runtime {

    /** Thread factory used to create the long-running worker thread. */
    private final ThreadFactory threadFactory;

    /**
     * Constructs a new runtime using the supplied virtual thread builder.
     *
     * @param virtualThreadBuilder virtual thread builder to derive a thread factory from
     */
    DefaultRuntime(Thread.Builder.OfVirtual virtualThreadBuilder) {
      this.threadFactory = virtualThreadBuilder.factory();
    }

    /** {@inheritDoc} */
    @Override
    public ThreadFactory workerThreadFactory() {
      return threadFactory;
    }

    /** {@inheritDoc} */
    @Override
    public ServerSocketChannel serverSocketChannel() throws IOException {
      return ServerSocketChannel.open(UNIX);
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteIfExists(Path path) throws IOException {
      return java.nio.file.Files.deleteIfExists(path);
    }
  }

  /**
   * Thrown when the health socket server does not reach the expected status within the specified
   * timeout.
   */
  public static class ServerStatusTimeoutException extends RuntimeException {

    /**
     * Constructs a new exception for a server status timeout.
     *
     * @param expectedStatus the status the server was expected to reach
     * @param timeout the maximum amount of time allowed to reach the expected status
     * @param currentStatus the status of the server when the timeout occurred
     */
    public ServerStatusTimeoutException(
        HealthSocketServer.Status expectedStatus,
        Duration timeout,
        HealthSocketServer.Status currentStatus) {
      super(
          "Did not reach status "
              + expectedStatus
              + " within "
              + timeout.toMillis()
              + " milliseconds, current status: "
              + currentStatus);
    }
  }

  /** Worker process responsible for accepting connections on the Unix socket. */
  class WorkerProcess implements Runnable {

    /** Scoped value holding the reference to the thread running this runnable. */
    private static final ScopedValue<Thread> THREAD = ScopedValue.newInstance();

    /** Counts consecutive client write failures. */
    private final AtomicInteger failedWriteCount = new AtomicInteger(0);

    /** Runs the worker process. */
    @Override
    public void run() {
      ScopedValue.where(THREAD, Thread.currentThread()).run(this::runListenerLoop);
    }

    /**
     * Runs the worker process.
     *
     * <ul>
     *   <li>Creates and binds the Unix socket.
     *   <li>Continuously listens for incoming connections until interrupted.
     *   <li>Handles interruption and I/O failures.
     *   <li>Cleans up the socket resources when the worker terminates.
     * </ul>
     */
    private void runListenerLoop() {
      try (var socket = socket()) {
        bind(socket);
        while (isNotInterrupted()) {
          listenOn(socket);
        }
      } catch (ClosedByInterruptException e) {
        log.debug(LOG_MESSAGE_EXCEPTION_OCCURRED, e);
      } catch (IOException e) {
        log.error(LOG_MESSAGE_EXCEPTION_OCCURRED, e);
      } finally {
        cleanUp();
      }
    }

    /**
     * Opens the server socket channel.
     *
     * @return the opened server socket channel
     * @throws IOException if an I/O error occurs while opening the channel
     */
    private ServerSocketChannel socket() throws IOException {
      return runtime.serverSocketChannel();
    }

    /**
     * Binds the server socket channel's socket to a local address.
     *
     * @param serverSocket the server socket channel to bind
     * @throws IOException if an I/O error occurs while binding the socket
     */
    private void bind(ServerSocketChannel serverSocket) throws IOException {
      var address = UnixDomainSocketAddress.of(socketPath); // Creates the socket address
      serverSocket.bind(address); // Binds the socket to the address
      setSocketReady(true); // Updates the socket ready flag

      log.info("Listening on: {}", socketPath);
    }

    /**
     * Tests whether the runner thread has been interrupted.
     *
     * @return {@code true} if the thread has not been interrupted; {@code false} otherwise
     */
    private boolean isNotInterrupted() {
      return !THREAD.get().isInterrupted();
    }

    /**
     * Accepts a single client connection and processes it.
     *
     * @param serverSocket the server socket channel to accept connections from
     * @throws IOException if an I/O error occurs
     */
    private void listenOn(ServerSocketChannel serverSocket) throws IOException {
      try (var clientSocket = serverSocket.accept()) {
        log.debug("Client connected");
        processConnection(clientSocket);
      }
    }

    /**
     * Handles a single client socket connection.
     *
     * <p>The method maps the current actuator health to one response byte: {@link
     * ApplicationStatus#HEALTHY} for {@link org.springframework.boot.health.contributor.Status#UP
     * }, otherwise {@link ApplicationStatus#UNHEALTHY}.
     *
     * <p>Any exception while reading health or writing to the socket results in an {@link
     * ApplicationStatus#UNHEALTHY} response.
     *
     * @param socket accepted client socket
     */
    private void processConnection(SocketChannel socket) {
      ApplicationStatus response;

      try {
        var health = healthEndpoint.health();
        var healthStatus = health.getStatus();
        response = UP.equals(healthStatus) ? HEALTHY : UNHEALTHY;
      } catch (Exception e) {
        log.error(LOG_MESSAGE_EXCEPTION_OCCURRED, e);
        response = UNHEALTHY;
      }

      try {
        socket.write(ByteBuffer.wrap(String.valueOf(response.statusChar()).getBytes(US_ASCII)));
        log.debug("Response sent: '{}' ({})", response.statusChar(), response.statusText());
        failedWriteCount.set(0);
      } catch (IOException e) {
        var currentFailedCount = failedWriteCount.incrementAndGet();
        if (currentFailedCount > FAILED_WRITE_COUNT_THRESHOLD) {
          log.warn("Failed to write response, consecutive failures: {}", currentFailedCount, e);
        } else {
          log.debug("Failed to write response, consecutive failures: {}", currentFailedCount, e);
        }
      }
    }

    /** Performs cleanup. */
    private void cleanUp() {
      closeThread();
      setSocketReady(false);
      cleanUpSocketFile();
      removeThreadReference();
    }

    /**
     * Updates the socket ready flag and signals any waiting threads about the status change.
     *
     * @param socketReadyStatus new value of the socket ready flag
     */
    private void setSocketReady(boolean socketReadyStatus) {
      var previous = socketReady.getAndSet(socketReadyStatus);
      if (previous != socketReadyStatus) {
        signalStatusChange();
      }
    }

    /** Ensures the worker thread is marked as interrupted. */
    private void closeThread() {
      THREAD.get().interrupt();
      log.debug("Interrupted");
    }

    /**
     * Removes the Unix domain socket file from the filesystem, if it exists.
     *
     * <p>Cleanup failures are intentionally non-fatal and only logged because stale socket files do
     * not affect the current JVM process once shutdown is in progress.
     */
    private void cleanUpSocketFile() {
      try {
        var result = runtime.deleteIfExists(socketPath);
        if (result) {
          log.debug("Socket file deleted: {}", socketPath);
        } else {
          log.debug("Socket file does not exist: {}", socketPath);
        }
      } catch (IOException e) {
        log.warn("Socket file deletion failed: {}", socketPath, e);
      }
    }

    /**
     * Removes the reference to the worker thread and signals any waiting threads about the status
     * change.
     */
    private void removeThreadReference() {
      workerThreadReference.set(null);
      signalStatusChange();
      log.info("Stopped");
    }
  }
}
