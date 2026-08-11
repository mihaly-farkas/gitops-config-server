package io.github.mihaly_farkas.gitops_config_server;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.boot.health.contributor.Status.DOWN;
import static org.springframework.boot.health.contributor.Status.OUT_OF_SERVICE;
import static org.springframework.boot.health.contributor.Status.UNKNOWN;
import static org.springframework.boot.health.contributor.Status.UP;

import java.io.IOException;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.actuate.endpoint.IndicatedHealthDescriptor;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles({"public_github_repo", "health_socket_server"})
class HealthSocketTest {

  static final char HEALTHY = 'H';

  static final char UNHEALTHY = 'U';

  static final Duration SOCKET_READ_DEFAULT_TIMEOUT = Duration.ofSeconds(1);

  static final Path socketPath;

  static {
    var tempDir = System.getProperty("java.io.tmpdir");
    var socketPathname = tempDir + "/health-" + UUID.randomUUID() + ".sock";
    socketPath = Path.of(socketPathname);
  }

  @MockitoBean private HealthEndpoint actuatorHealthEndpoint;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("gitops-config-server.health.socket.path", socketPath::toString);
  }

  @AfterAll
  @SneakyThrows
  static void cleanUp() {
    Files.deleteIfExists(socketPath);
  }

  public static Stream<Arguments> unhealthyStatuses() {
    return Stream.of(Arguments.of(DOWN), Arguments.of(OUT_OF_SERVICE), Arguments.of(UNKNOWN));
  }

  @DisplayName("Health socket responds with 'H' (HEALTHY) when the health status is UP")
  @Test
  @SneakyThrows
  void serverIsHealthy() {
    // ARRANGE
    var healthDescriptor = mock(IndicatedHealthDescriptor.class);

    when(healthDescriptor.getStatus()).thenReturn(UP);
    when(actuatorHealthEndpoint.health()).thenReturn(healthDescriptor);

    try (var socket = open(socketPath)) {
      // ACT
      char response = readSingleByteWithTimeout(socket);

      // ASSERT
      assertThat(response, is(HEALTHY));
    }
  }

  @DisplayName("Health socket responds with 'U' (UNHEALTHY) when the health status is not UP")
  @MethodSource("unhealthyStatuses")
  @ParameterizedTest(name = "status={0}")
  @SneakyThrows
  void serverIsUnhealthy(Status status) {
    // ARRANGE
    var healthDescriptor = mock(IndicatedHealthDescriptor.class);

    when(healthDescriptor.getStatus()).thenReturn(status);
    when(actuatorHealthEndpoint.health()).thenReturn(healthDescriptor);

    try (var socket = open(socketPath)) {
      // ACT
      char response = readSingleByteWithTimeout(socket);

      // ASSERT
      assertThat(response, is(UNHEALTHY));
    }
  }

  @DisplayName(
      "Health socket responds with 'U' (UNHEALTHY) when the health endpoint throws an exception")
  @Test
  @SneakyThrows
  void serverIsUnhealthyWhenExceptionThrown() {
    // ARRANGE
    when(actuatorHealthEndpoint.health()).thenThrow(new RuntimeException("Simulated exception"));

    try (var socket = open(socketPath)) {
      // ACT
      char response = readSingleByteWithTimeout(socket);

      // ASSERT
      assertThat(response, is(UNHEALTHY));
    }
  }

  @DisplayName(
      "Health socket read operation cancelled on the first request, but succeeds on the second request")
  @Test
  @SneakyThrows
  void readAfterTimeout() {
    // ARRANGE
    var healthDescriptor = mock(IndicatedHealthDescriptor.class);

    when(healthDescriptor.getStatus()).thenReturn(UP);
    when(actuatorHealthEndpoint.health()).thenReturn(healthDescriptor);

    // Opening and closing the socket immediately to simulate a timeout on the first read attempt
    // This causes a "java.io.IOException: Broken pipe" exception  on the server side, which should
    // be handled gracefully, allowing the next read attempt to succeed.
    open(socketPath).close();

    try (var socket = open(socketPath)) {
      // ACT
      char response = readSingleByteWithTimeout(socket);

      // ASSERT
      assertThat(response, is(HEALTHY));
    }
  }

  private static SocketChannel open(Path socketPath) throws IOException {
    var socketAddress = UnixDomainSocketAddress.of(socketPath);
    return SocketChannel.open(socketAddress);
  }

  @SneakyThrows
  char readSingleByteWithTimeout(SocketChannel socketChannel) {
    return readSingleByteWithTimeout(socketChannel, SOCKET_READ_DEFAULT_TIMEOUT);
  }

  @SneakyThrows
  char readSingleByteWithTimeout(SocketChannel socketChannel, Duration timeout) {
    var buffer = ByteBuffer.allocate(1);

    socketChannel.configureBlocking(false);
    long deadlineNanos = System.nanoTime() + timeout.toNanos();

    while (System.nanoTime() < deadlineNanos) {
      int bytesRead = socketChannel.read(buffer);
      if (bytesRead != 0) {
        assertThat(bytesRead, is(1));
        buffer.flip();
        return (char) buffer.get();
      }
    }

    throw new RuntimeException("Timeout while waiting for a byte to be read from the socket");
  }
}
