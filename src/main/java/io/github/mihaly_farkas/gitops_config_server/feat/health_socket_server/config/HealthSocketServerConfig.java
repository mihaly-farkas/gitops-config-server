package io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.config;

import io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component.HealthSocketServer;
import io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component.RespondCommand;
import io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component.RunCommand;
import io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component.StopCommand;
import io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component.default_command.DefaultRespondCommand;
import io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component.default_command.DefaultRunCommand;
import io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component.default_command.DefaultStopCommand;
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
  RespondCommand respondCommand(HealthEndpoint healthEndpoint) {
    return new DefaultRespondCommand(healthEndpoint);
  }

  @Bean
  RunCommand runCommand(RespondCommand respondCommand) {
    return new DefaultRunCommand(respondCommand);
  }

  @Bean
  StopCommand stopCommand() {
    return new DefaultStopCommand();
  }

  @Bean
  public HealthSocketServer healthSocketServer(
      @Value("${gitops-config-server.health.socket.path}") String socketPath,
      RunCommand runCommand,
      StopCommand stopCommand) {
    return new HealthSocketServer(
        socketPath,
        Thread.ofVirtual().name("health-socket-server"),
        new AtomicReference<>(),
        runCommand,
        stopCommand);
  }
}
