package io.github.mihaly_farkas.gitops_config_server;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.boot.health.contributor.Status.DOWN;
import static org.springframework.boot.health.contributor.Status.OUT_OF_SERVICE;
import static org.springframework.boot.health.contributor.Status.UNKNOWN;
import static org.springframework.boot.health.contributor.Status.UP;

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

  private static final Duration SOCKET_READ_TIMEOUT = Duration.ofSeconds(1);

  private static final Path socketPath =
      Path.of(System.getProperty("java.io.tmpdir"), "health-" + UUID.randomUUID() + ".sock");
  @MockitoBean private HealthEndpoint actuatorHealthEndpoint;

  @DynamicPropertySource
  static void healthSocketProperties(DynamicPropertyRegistry registry) {
    registry.add("gitops-config-server.health.socket.path", socketPath::toString);
  }

  @AfterAll
  @SneakyThrows
  static void cleanupSocketFile() {
    Files.deleteIfExists(socketPath);
  }

  public static Stream<Arguments> unhealthyStatuses() {
    return Stream.of(Arguments.of(DOWN), Arguments.of(OUT_OF_SERVICE), Arguments.of(UNKNOWN));
  }

  @DisplayName("Health socket responds with 'H' (HEALTHY) when the health status is UP")
  @Test
  @SneakyThrows
  void healthSocketServerIsRunning() {
    // ARRANGE
    var healthDescriptor = mock(IndicatedHealthDescriptor.class);
    when(healthDescriptor.getStatus()).thenReturn(UP);
    when(actuatorHealthEndpoint.health()).thenReturn(healthDescriptor);
    var socketAddress = UnixDomainSocketAddress.of(socketPath);

    // ACT & ASSERT
    try (var socketChannel = SocketChannel.open(socketAddress)) {
      var buffer = ByteBuffer.allocate(1);
      int bytesRead = readSingleByteWithTimeout(socketChannel, buffer);
      assertThat(bytesRead, is(1));
      buffer.flip();

      char response = (char) buffer.get();
      assertThat(response, is('H'));
    }
  }

  @DisplayName("Health socket responds with 'U' (UNHEALTHY) when the health status is not UP")
  @MethodSource("unhealthyStatuses")
  @ParameterizedTest(name = "status={0}")
  @SneakyThrows
  void healthSocketServerIsUnhealthy(Status status) {
    // ARRANGE
    var healthDescriptor = mock(IndicatedHealthDescriptor.class);
    when(healthDescriptor.getStatus()).thenReturn(status);
    when(actuatorHealthEndpoint.health()).thenReturn(healthDescriptor);

    var socketAddress = UnixDomainSocketAddress.of(socketPath);

    // ACT & ASSERT
    try (var socketChannel = SocketChannel.open(socketAddress)) {
      var buffer = ByteBuffer.allocate(1);
      int bytesRead = readSingleByteWithTimeout(socketChannel, buffer);
      assertThat(bytesRead, is(1));
      buffer.flip();

      char response = (char) buffer.get();
      assertThat(response, is('U'));
    }
  }

  @SneakyThrows
  int readSingleByteWithTimeout(SocketChannel socketChannel, ByteBuffer buffer) {
    socketChannel.configureBlocking(false);
    long deadlineNanos = System.nanoTime() + SOCKET_READ_TIMEOUT.toNanos();

    while (System.nanoTime() < deadlineNanos) {
      int bytesRead = socketChannel.read(buffer);
      if (bytesRead != 0) {
        return bytesRead;
      }
    }

    return -1;
  }
}
