package io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component;

import static java.net.StandardProtocolFamily.UNIX;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
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
public class HealthSocketServer implements AutoCloseable {

  /** Filesystem path of the Unix domain socket used for health probes. */
  private final String socketFilename;

  private final Thread.Builder.OfVirtual virtualThreadBuilder;

  /** Background virtual thread serving health socket probes. */
  private final AtomicReference<Thread> serverThread;

  private final RunCommand runCommand;

  private final StopCommand stopCommand;

  /** Starts the health socket server on a virtual thread after bean initialization. */
  @PostConstruct
  void start() {
    log.info("Starting health socket server on path: {}", socketFilename);
    serverThread.set(virtualThreadBuilder.start(this::run));
    log.info("Health socket server started successfully");
  }

  /** Stops the virtual-thread server loop during bean destruction. */
  @PreDestroy
  @Override
  public void close() {
    var thread = serverThread.getAndSet(null);
    log.info("Stopping health socket server on path: {}", socketFilename);
    stopCommand.apply(thread, socketFilename);
    log.info("Health socket server stopped successfully");
  }

  /**
   * Runs the socket server loop, accepts client connections, and returns the current health status.
   */
  private void run() {
    var currentThread = Thread.currentThread();
    try (var server = ServerSocketChannel.open(UNIX)) {
      runCommand.apply(currentThread, server, socketFilename);
    } catch (Exception exception) {
      log.error("Error occurred while running health socket server", exception);
    } finally {
      close();
    }
  }
}
