package io.github.mihaly_farkas.spring_cloud_config_server;

import io.github.mihaly_farkas.spring_cloud_config_server.system.ConfigDebugListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication
@EnableConfigServer
public class SpringCloudConfigServer {
  static void main(String[] args) {
    SpringApplication application = new SpringApplication(SpringCloudConfigServer.class);
    application.addListeners(new ConfigDebugListener());
    application.run(args);
  }
}
