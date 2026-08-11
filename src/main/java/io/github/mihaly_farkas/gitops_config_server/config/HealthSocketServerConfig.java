package io.github.mihaly_farkas.gitops_config_server.config;

import io.github.mihaly_farkas.gitops_config_server.system.HealthSocketServer;
import java.util.concurrent.atomic.AtomicReference;
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
      @Value("${gitops-config-server.health.socket.path}") String socketPath,
      HealthEndpoint actuatorHealthEndpoint) {
    return new HealthSocketServer(
        socketPath, actuatorHealthEndpoint, new AtomicReference<>(), null);
  }
}
