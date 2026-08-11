package io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component.default_command;

import io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component.RespondCommand;
import io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component.RunCommand;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.ServerSocketChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class DefaultRunCommand implements RunCommand {

  private final RespondCommand respondCommand;

  @Override
  public void apply(Thread thread, ServerSocketChannel server, String socketFilename)
      throws Exception {
    var address = UnixDomainSocketAddress.of(socketFilename);
    server.bind(address);

    while (!thread.isInterrupted()) {
      try (var client = server.accept()) {
        respondCommand.apply(client);
      }
    }
  }
}
