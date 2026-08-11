package io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.config;

import io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component.HealthSocketServer;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "gitops-config-server.health.socket.enabled", havingValue = "true")
public class HealthSocketServerConfig {

  @Bean
  public HealthSocketServer healthSocketServer(
      HealthEndpoint healthEndpoint,
      @Value("${gitops-config-server.health.socket.path:health.sock}") String socketPathString) {
    var socketPath = Path.of(socketPathString);
    return HealthSocketServer.builder()
        .socketPath(socketPath)
        .healthEndpoint(healthEndpoint)
        .build();
  }
}
