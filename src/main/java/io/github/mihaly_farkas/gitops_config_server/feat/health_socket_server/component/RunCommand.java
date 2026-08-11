package io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component;

import java.nio.channels.ServerSocketChannel;

@FunctionalInterface
public interface RunCommand {
  void apply(Thread thread, ServerSocketChannel server, String socketFilename) throws Exception;
}
