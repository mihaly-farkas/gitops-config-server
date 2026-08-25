package io.github.mihaly_farkas.spring_cloud_config_server.lib.test_tool;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import lombok.SneakyThrows;

public final class UnixSocketReader {

  public static final Duration DEFAULT_SOCKET_READ_TIMEOUT = Duration.ofSeconds(1);

  private UnixSocketReader() {
    // private constructor to prevent instantiation
  }

  @SneakyThrows
  public static Optional<Character> readFrom(Path socketPath) {
    return readFrom(socketPath, DEFAULT_SOCKET_READ_TIMEOUT);
  }

  @SneakyThrows
  public static Optional<Character> readFrom(Path socketPath, Duration timeout) {
    var socketAddress = UnixDomainSocketAddress.of(socketPath);
    try (var socketChannel = SocketChannel.open(socketAddress)) {
      var buffer = ByteBuffer.allocate(1);

      socketChannel.configureBlocking(false);
      long deadlineNanos = System.nanoTime() + timeout.toNanos();

      while (System.nanoTime() < deadlineNanos) {
        int bytesRead = socketChannel.read(buffer);
        if (bytesRead != 0) {
          assertThat(bytesRead, is(1));
          buffer.flip();
          return Optional.of((char) buffer.get());
        }
      }
    }
    return Optional.empty();
  }
}
