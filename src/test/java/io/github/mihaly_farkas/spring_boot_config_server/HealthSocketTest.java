package io.github.mihaly_farkas.spring_boot_config_server;

import static io.github.mihaly_farkas.spring_boot_config_server.lib.test_tool.UnixSocketReader.readFrom;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.boot.health.contributor.Status.DOWN;
import static org.springframework.boot.health.contributor.Status.OUT_OF_SERVICE;
import static org.springframework.boot.health.contributor.Status.UNKNOWN;
import static org.springframework.boot.health.contributor.Status.UP;

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

  static final Path socketPath;

  static {
    var socketPathname = "health-" + UUID.randomUUID() + ".sock";
    socketPath = Path.of(socketPathname);
  }

  @MockitoBean private HealthEndpoint actuatorHealthEndpoint;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "mihaly-farkas.spring-boot-config-server.health.socket.path", socketPath::toString);
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
  @SuppressWarnings("OptionalGetWithoutIsPresent")
  void serverIsHealthy() {
    // ARRANGE
    var healthDescriptor = mock(IndicatedHealthDescriptor.class);

    when(healthDescriptor.getStatus()).thenReturn(UP);
    when(actuatorHealthEndpoint.health()).thenReturn(healthDescriptor);

    // ACT
    var status = readFrom(socketPath); // Read from the socket
    var statusChar = status.get(); // Get the character from the Optional

    // ASSERT
    assertThat(statusChar, is('H'));
  }

  @DisplayName("Health socket responds with 'U' (UNHEALTHY) when the health status is not UP")
  @MethodSource("unhealthyStatuses")
  @ParameterizedTest(name = "status={0}")
  @SneakyThrows
  @SuppressWarnings("OptionalGetWithoutIsPresent")
  void serverIsUnhealthy(Status actuatorStatus) {
    // ARRANGE
    var healthDescriptor = mock(IndicatedHealthDescriptor.class);

    when(healthDescriptor.getStatus()).thenReturn(actuatorStatus);
    when(actuatorHealthEndpoint.health()).thenReturn(healthDescriptor);

    // ACT
    var status = readFrom(socketPath); // Read from the socket
    var statusChar = status.get(); // Get the character from the Optional

    // ASSERT
    assertThat(statusChar, is('U'));
  }

  @DisplayName(
      "Health socket responds with 'U' (UNHEALTHY) when the health endpoint throws an exception")
  @Test
  @SneakyThrows
  @SuppressWarnings("OptionalGetWithoutIsPresent")
  void serverIsUnhealthyWhenExceptionThrown() {
    // ARRANGE
    when(actuatorHealthEndpoint.health()).thenThrow(new RuntimeException("Simulated exception"));

    // ACT
    var status = readFrom(socketPath); // Read from the socket
    var statusChar = status.get(); // Get the character from the Optional

    // ASSERT
    assertThat(statusChar, is('U'));
  }

  @DisplayName(
      "Health socket read operation cancelled on the first request, but succeeds on the second request")
  @Test
  @SneakyThrows
  @SuppressWarnings("OptionalGetWithoutIsPresent")
  void readAfterTimeout() {
    // ARRANGE
    var healthDescriptor = mock(IndicatedHealthDescriptor.class);

    when(healthDescriptor.getStatus()).thenReturn(UP);
    when(actuatorHealthEndpoint.health()).thenReturn(healthDescriptor);

    // ACT
    readFrom(socketPath, Duration.ofNanos(1)); // First read with a very short timeout
    var status = readFrom(socketPath); // Second read
    var statusChar = status.get(); // Get the character from the Optional

    // ASSERT
    assertThat(statusChar, is('H'));
  }
}
