package io.github.mihaly_farkas.gitops_config_server;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
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
import java.nio.file.Path;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.actuate.endpoint.IndicatedHealthDescriptor;
import org.springframework.boot.health.contributor.Status;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles({"mockmvc_public_github_repo", "mockmvc_health_socket_server"})
class HealthSocketTest extends MockmvcTest {

  @MockitoBean private HealthEndpoint actuatorHealthEndpoint;

  public static Stream<Arguments> unhealthyStatuses() {
    return Stream.of(Arguments.of(DOWN), Arguments.of(OUT_OF_SERVICE), Arguments.of(UNKNOWN));
  }

  @Test
  @DisplayName(
      "The application responds to health socket request with 'H' (HEALTHY)"
          + " when the health status is UP")
  @SneakyThrows
  void healthSocketServerIsRunning() {
    // ARRANGE
    var healthDescriptor = mock(IndicatedHealthDescriptor.class);
    when(healthDescriptor.getStatus()).thenReturn(UP);
    when(actuatorHealthEndpoint.health()).thenReturn(healthDescriptor);

    var socketPath = Path.of("health.sock");
    var socketAddress = UnixDomainSocketAddress.of(socketPath);

    // ACT & ASSERT
    try (var socketChannel = SocketChannel.open(socketAddress)) {
      var buffer = ByteBuffer.allocate(1);
      int bytesRead = socketChannel.read(buffer);
      assertThat(bytesRead, is(1));
      buffer.flip();

      char response = (char) buffer.get();
      assertThat(response, is(equalTo('H')));
    }
  }

  @ParameterizedTest(name = "status={0}")
  @MethodSource("unhealthyStatuses")
  @DisplayName(
      "The application responds to health socket request with 'U' (UNHEALTHY)"
          + " when the health status is not UP")
  @SneakyThrows
  void healthSocketServerIsUnhealthy(Status status) {
    // ARRANGE
    var healthDescriptor = mock(IndicatedHealthDescriptor.class);
    when(healthDescriptor.getStatus()).thenReturn(status);
    when(actuatorHealthEndpoint.health()).thenReturn(healthDescriptor);

    var socketPath = Path.of("health.sock");
    var socketAddress = UnixDomainSocketAddress.of(socketPath);

    // ACT & ASSERT
    try (var socketChannel = SocketChannel.open(socketAddress)) {
      var buffer = ByteBuffer.allocate(1);
      int bytesRead = socketChannel.read(buffer);
      assertThat(bytesRead, is(1));
      buffer.flip();

      char response = (char) buffer.get();
      assertThat(response, is(equalTo('U')));
    }
  }
}
