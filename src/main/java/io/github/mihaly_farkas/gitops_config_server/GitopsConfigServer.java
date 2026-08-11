package io.github.mihaly_farkas.gitops_config_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/** Entry point for the GitOps Config Server Spring Boot application. */
@SpringBootApplication
@EnableConfigServer
public class GitopsConfigServer {

  /**
   * Starts the Spring Boot application.
   *
   * @param args command-line arguments passed to the application
   */
  static void main(String... args) {
    SpringApplication application = new SpringApplication(GitopsConfigServer.class);
    application.run(args);
  }
}
