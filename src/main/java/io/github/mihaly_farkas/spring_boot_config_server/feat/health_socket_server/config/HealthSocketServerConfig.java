package io.github.mihaly_farkas.spring_boot_config_server.feat.health_socket_server.config;

import io.github.mihaly_farkas.spring_boot_config_server.feat.health_socket_server.component.HealthSocketServer;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot configuration for the Unix domain socket health server.
 *
 * <p>This configuration is conditional on the property {@code
 * spring-cloud-config-server.health.socket.enabled} being set to {@code true}. When enabled, it
 * creates a {@link HealthSocketServer} bean that listens on the configured Unix domain socket path
 * for incoming health check connections.
 *
 * @author Mihály Farkas
 * @see HealthSocketServer
 */
@Configuration
@ConditionalOnProperty(
    value = "mihaly-farkas.spring-cloud-config-server.health.socket.enabled",
    havingValue = "true")
public class HealthSocketServerConfig {

  /**
   * Creates and configures the health socket server bean.
   *
   * <p>The socket server will listen on the Unix domain socket path specified by the property
   * {@code mihaly-farkas.spring-cloud-config-server.health.socket.path} (defaults to {@code
   * health.sock} in the current working directory).
   *
   * @param healthEndpoint the Spring Boot Actuator health endpoint used to obtain health status
   * @param socketPathString the filesystem path for the Unix domain socket (defaults to {@code
   *     health.sock})
   * @return a configured and ready-to-use {@link HealthSocketServer} instance
   */
  @Bean
  public HealthSocketServer healthSocketServer(
      HealthEndpoint healthEndpoint,
      @Value("${mihaly-farkas.spring-cloud-config-server.health.socket.path:health.sock}")
          String socketPathString) {
    var socketPath = Path.of(socketPathString);
    return HealthSocketServer.builder()
        .socketPath(socketPath)
        .healthEndpoint(healthEndpoint)
        .build();
  }
}
