package io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component;

@FunctionalInterface
public interface StopCommand {
  void apply(Thread thread, String socketFilename);
}
