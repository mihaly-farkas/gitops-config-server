package io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component.default_command;

import static java.nio.file.Files.deleteIfExists;

import io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component.StopCommand;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultStopCommand implements StopCommand {

  @Override
  public void apply(Thread thread, String socketFilename) {
    if (thread != null) {
      log.info("Stopping health socket server on path: {}", socketFilename);

      thread.interrupt();

      try {
        thread.join();
        log.info("Health socket server stopped successfully");
      } catch (Exception exception) {
        Thread.currentThread().interrupt();
        log.warn("Failed to stop health socket server gracefully: {}", exception.getMessage());
      } finally {
        cleanupSocketFile(socketFilename);
      }
    }
  }

  /**
   * Cleans up the Unix domain socket file on shutdown to prevent stale socket files from
   * persisting.
   */
  private void cleanupSocketFile(String socketFilename) {
    if (socketFilename != null) {
      try {
        deleteIfExists(Path.of(socketFilename));
      } catch (Exception exception) {
        log.warn(
            "Failed to clean up socket file at {}: {}", socketFilename, exception.getMessage());
      }
    }
  }
}
