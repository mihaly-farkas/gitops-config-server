package io.github.mihaly_farkas.spring_cloud_config_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/** Entry point for the Spring Cloud Config Server Spring Boot application. */
@SpringBootApplication
@EnableConfigServer
public class SpringCloudConfigServer {

  /**
   * Starts the Spring Boot application.
   *
   * @param args command-line arguments passed to the application
   */
  static void main(String... args) {
    SpringApplication application = new SpringApplication(SpringCloudConfigServer.class);
    application.run(args);
  }
}
