package io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component.default_command;

import static org.springframework.boot.health.contributor.Status.UP;

import io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component.RespondCommand;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;

@RequiredArgsConstructor
@Slf4j
public class DefaultRespondCommand implements RespondCommand {

  /** Response byte representing a healthy application state. */
  private static final char HEALTHY = 'H';

  /** Response byte representing an unhealthy application state. */
  private static final char UNHEALTHY = 'U';

  /** Actuator endpoint used to obtain the current application health status. */
  private final HealthEndpoint healthEndpoint;

  /**
   * Writes the current health status to the connected client.
   *
   * @param client connected socket channel
   */
  @Override
  @SneakyThrows
  public void apply(SocketChannel client) {
    var health = healthEndpoint.health();
    var response = health.getStatus().equals(UP) ? HEALTHY : UNHEALTHY;
    log.debug("Health status: {}", health.getStatus());

    try {
      client.write(ByteBuffer.wrap(new byte[] {(byte) response}));
    } catch (IOException ioException) {
      log.error("Error while writing response", ioException);
    }
  }
}
