package io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component;

import java.nio.channels.SocketChannel;

@FunctionalInterface
public interface RespondCommand {
  void apply(SocketChannel client);
}
