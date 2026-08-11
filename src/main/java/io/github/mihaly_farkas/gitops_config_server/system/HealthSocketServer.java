package io.github.mihaly_farkas.gitops_config_server.system;

import static java.net.StandardProtocolFamily.UNIX;
import static java.nio.file.Files.deleteIfExists;
import static org.springframework.boot.health.contributor.Status.UP;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
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
 * <p>Connections are served on a virtual thread to keep startup non-blocking and to efficiently
 * handle concurrent probes.
 */
@RequiredArgsConstructor
@Slf4j
public class HealthSocketServer {

  /** Response byte representing a healthy application state. */
  private static final char HEALTHY = 'H';

  /** Response byte representing an unhealthy application state. */
  private static final char UNHEALTHY = 'U';

  /** Filesystem path of the Unix domain socket used for health probes. */
  private final String socketPath;

  /** Actuator endpoint used to obtain the current application health status. */
  private final HealthEndpoint actuatorHealthEndpoint;

  /** Background virtual thread serving health socket probes. */
  private final AtomicReference<Thread> serverThread;

  @Nullable private final Thread.UncaughtExceptionHandler customUncaughtExceptionHandler;

  /** Starts the health socket server on a virtual thread after bean initialization. */
  @PostConstruct
  void start() {
    log.info("Starting health socket server on path: {}", socketPath);
    var virtualThreadBuilder = Thread.ofVirtual();
    if (customUncaughtExceptionHandler != null) {
      virtualThreadBuilder.uncaughtExceptionHandler(customUncaughtExceptionHandler);
    }
    serverThread.set(virtualThreadBuilder.start(this::run));
  }

  /** Stops the virtual-thread server loop during bean destruction. */
  @PreDestroy
  void stop() {
    var thread = serverThread.getAndSet(null);
    if (thread != null) {
      log.info("Stopping health socket server on path: {}", socketPath);

      thread.interrupt();

      try {
        thread.join();
        log.info("Health socket server stopped successfully");
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        log.warn("Interrupted while waiting for health socket server shutdown", exception);
      } finally {
        cleanupSocketFile();
      }
    }
  }

  /**
   * Runs the socket server loop, accepts client connections, and returns the current health status.
   */
  @SneakyThrows
  private void run() {
    var address = UnixDomainSocketAddress.of(socketPath);

    try (var server = ServerSocketChannel.open(UNIX)) {
      server.bind(address);

      while (!Thread.currentThread().isInterrupted()) {
        try (var client = server.accept()) {
          respond(client);
        }
      }
    } catch (ClosedByInterruptException _) {
      Thread.currentThread().interrupt();
      log.warn("Health socket server interrupted; shutting down");
    } catch (IOException exception) {
      throw new RuntimeException("Error while running health socket server", exception);
    }
  }

  /**
   * Writes the current health status to the connected client.
   *
   * @param client connected socket channel
   */
  @SneakyThrows
  private void respond(SocketChannel client) {
    var health = actuatorHealthEndpoint.health();
    var response = health.getStatus().equals(UP) ? HEALTHY : UNHEALTHY;
    log.debug("Health status: {}", health.getStatus());

    try {
      client.write(ByteBuffer.wrap(new byte[] {(byte) response}));
    } catch (IOException _) {
      log.warn("Health socket client disconnected before response could be sent");
    }
  }

  /**
   * Cleans up the Unix domain socket file on shutdown to prevent stale socket files from
   * persisting.
   */
  @SneakyThrows
  private void cleanupSocketFile() {
    if (socketPath != null) {
      try {
        deleteIfExists(Path.of(socketPath));
      } catch (IOException e) {
        log.warn("Failed to clean up socket file at {}: {}", socketPath, e.getMessage());
      }
    }
  }
}
