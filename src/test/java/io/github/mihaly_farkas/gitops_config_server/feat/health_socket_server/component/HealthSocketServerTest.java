package io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component;

import static ch.qos.logback.classic.Level.DEBUG;
import static ch.qos.logback.classic.Level.ERROR;
import static ch.qos.logback.classic.Level.INFO;
import static ch.qos.logback.classic.Level.WARN;
import static io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component.HealthSocketServer.Status.RUNNING;
import static io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component.HealthSocketServer.Status.STOPPED;
import static io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component.HealthSocketServer.Status.STOPPING;
import static io.github.mihaly_farkas.gitops_config_server.lib.test_tool.MockitoHelper.assertVerify;
import static io.github.mihaly_farkas.gitops_config_server.lib.test_tool.TestLogAppenderMatcher.logEntry;
import static io.github.mihaly_farkas.gitops_config_server.lib.test_tool.TestLogAppenderMatcher.logged;
import static io.github.mihaly_farkas.gitops_config_server.lib.test_tool.TestLogAppenderMatcher.loggedInOrder;
import static io.github.mihaly_farkas.gitops_config_server.lib.test_tool.UnixSocketReader.readFrom;
import static java.lang.Thread.ofVirtual;
import static java.lang.Thread.sleep;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.health.contributor.Status.UP;

import io.github.mihaly_farkas.gitops_config_server.lib.test_tool.TestLogAppender;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.health.actuate.endpoint.HealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.actuate.endpoint.IndicatedHealthDescriptor;
import org.springframework.boot.health.contributor.Status;

class HealthSocketServerTest {

  static Stream<Arguments> healthStatuses() {
    return Stream.of(
        Arguments.of(UP, 'H'),
        Arguments.of(Status.DOWN, 'U'),
        Arguments.of(Status.OUT_OF_SERVICE, 'U'),
        Arguments.of(Status.UNKNOWN, 'U'));
  }

  @DisplayName(
      "HealthSocketServer responses based on the health status provided by the Actuator Health Endpoint")
  @MethodSource("healthStatuses")
  @ParameterizedTest(name = "actuatorStatus: {0}")
  @SneakyThrows
  @SuppressWarnings("OptionalGetWithoutIsPresent")
  void socketResponse(Status actuatorStatus, char expectedStatus) {
    // ARRANGE
    var socketPath = socketPath();
    var socketPathname = socketPath.toString();
    var healthDescriptor = mockHealthDescriptor(actuatorStatus);
    var healthEndpoint = mockHealthEndpoint(healthDescriptor);
    var server = serverBuilder().socketPath(socketPath).healthEndpoint(healthEndpoint).build();
    var logAppender = attachLogAppenderTo(server);

    // ACT
    server
        .start() // Start the server
        .waitUntil(RUNNING); // Wait until the server is running

    var status = readFrom(server.socketPath()); // Read the response through the socket

    server.stop(); // Stop the server

    // ASSERT
    assertThat(
        "The response from the health socket server is '" + expectedStatus + "'",
        status.get(),
        is(expectedStatus));
    assertThat(
        "The server logged the expected messages",
        logAppender,
        loggedInOrder(
            logEntry(
                INFO, "Health socket server listening (socketPathname=" + socketPathname + ")"),
            logEntry(DEBUG, "Health socket server accepted a connection"),
            logEntry(DEBUG, "Health socket server sent response (response=" + expectedStatus + ")"),
            logEntry(DEBUG, "Health socket server thread interrupted"),
            logEntry(
                DEBUG,
                "Health socket server cleaned up the socket file (socketPathname="
                    + socketPathname
                    + ")"),
            logEntry(INFO, "Health socket server stopped")));
  }

  @DisplayName("HealthSocketServer cannot be created with a null socket path")
  @Test
  void nullSocketPath() {
    // ARRANGE
    var serverBuilder = serverBuilder().socketPath(null);

    // ACT & ASSERT
    var exception = assertThrowsExactly(IllegalArgumentException.class, serverBuilder::build);

    assertThat(
        "The exception message is as expected",
        exception.getMessage(),
        is("socketPath cannot be null"));
  }

  @DisplayName("HealthSocketServer cannot be created with a null health endpoint")
  @Test
  void nullHealthEndpoint() {
    // ARRANGE
    var serverBuilder = serverBuilder().healthEndpoint(null);

    // ACT & ASSERT
    var exception = assertThrowsExactly(IllegalArgumentException.class, serverBuilder::build);

    assertThat(
        "The exception message is as expected",
        exception.getMessage(),
        is("healthEndpoint cannot be null"));
  }

  @DisplayName("HealthSocketServer is an auto-closeable resource")
  @Test
  @SneakyThrows
  void autoCloseable() {
    // ARRANGE
    var server = server();

    // ACT & ASSERT
    try (server) {
      server
          .start() // Start the server
          .waitUntil(RUNNING); // Wait until the server is running

      // The server will be automatically closed at the end of the try-with-resources block
    }

    server.waitUntil(STOPPED);

    assertThat("The server is stopped", server.status(), is(STOPPED));
  }

  @DisplayName("HealthSocketServer.start() executes safely when called twice")
  @Test
  @SneakyThrows
  void startTwice() {
    // ARRANGE
    var server = server();
    var logAppender = attachLogAppenderTo(server);

    // ACT
    server
        .start() // Start the server for the first time
        .waitUntil(RUNNING) // Wait until the server is running
        .start() // Call start() again while the server is already running
        .stop(); // Stop the server

    logAppender.detach(); // Detach the log appender

    // ASSERT
    assertThat(
        "The server logged that it is already running",
        logAppender,
        logged(WARN, "Health socket server is already running (threadName=health-socket-server)"));
  }

  @DisplayName("HealthSocketServer.start() executes safely when called concurrently")
  @Test
  @SneakyThrows
  void startConcurrently() {
    // ARRANGE

    // Mock the server thread we want to be created
    var serverThread = mock(Thread.class);
    when(serverThread.getName()).thenReturn("health-socket-server-test-thread-" + randomUUID());

    // Mock the server thread that is set by another concurrent call to start()
    var serverThreadSetByOther = mock(Thread.class);
    when(serverThreadSetByOther.getName())
        .thenReturn("health-socket-server-test-thread-set-by-other-call-" + randomUUID());
    var serverThreadSetByOtherName = Optional.of(serverThreadSetByOther.getName());

    // Mock the AtomicReference holding the running server thread.
    //
    // Simulates a race condition where two threads call start() concurrently:
    // 1. The slower thread checks if the reference is null.
    // 2. A faster thread sets the reference in the meantime.
    // 3. The slower thread finds a non-null reference immediately after its check.
    //
    // Behavior setup:
    // - get(): First returns null (simulating no running thread), then returns the thread set by
    // the rival call.
    // - compareAndSet(null, serverThread): Returns false to simulate that another thread beat it to
    // the initialization.
    var serverThreadReference = mockServerThreadReference();
    when(serverThreadReference.get()).thenReturn(null, serverThreadSetByOther);
    when(serverThreadReference.compareAndSet(null, serverThread)).thenReturn(false);

    // Mock the server thread factory that creates new server threads
    // It returns a server thread for the slower starter that loses the race.
    var serverThreadFactory = mock(ThreadFactory.class);
    var runtime = mock(HealthSocketServer.Runtime.class);
    when(runtime.serverThreadFactory()).thenReturn(serverThreadFactory);
    when(serverThreadFactory.newThread(any(HealthSocketServer.ServerProcess.class)))
        .thenReturn(serverThread);

    // Create the HealthSocketServer instance
    var server =
        new HealthSocketServer(
            mock(Path.class), mock(HealthEndpoint.class), runtime, serverThreadReference);
    var logAppender = attachLogAppenderTo(server);

    // ACT
    server.start(); // Start the server

    logAppender.detach(); // Detach the log appender

    // ASSERT
    assertThat(
        "The server thread is the one which created by the faster starter",
        server.serverThreadName(),
        is(serverThreadSetByOtherName));
    assertVerify(
        "The server thread that was created by the slower starter is never started",
        () -> verify(serverThread, times(0)).start());
    assertThat(
        "The server logged that it is already running",
        logAppender,
        logged(
            WARN,
            "Health socket server is already running (threadName="
                + serverThreadSetByOtherName.get()
                + ")"));
  }

  @DisplayName(
      "HealthSocketServer.serverThreadName() returns the name of the running server thread")
  @Test
  @SneakyThrows
  @SuppressWarnings("resource")
  void serverThreadName() {
    // ARRANGE
    var serverThreadName = Optional.of("health-socket-server-test-thread-" + randomUUID());
    var virtualThreadBuilder = ofVirtual().name(serverThreadName.get());
    var server = serverBuilder().virtualThreadBuilder(virtualThreadBuilder).build();

    // ACT
    server
        .start() // Start the server
        .waitUntil(RUNNING); // Wait until the server is running

    var threadName = server.serverThreadName(); // Get the name of the running server thread

    server.stop(); // Stop the server

    // ASSERT
    assertThat(
        "The server thread name is the one that was set for the thread builder during creation",
        threadName,
        is(serverThreadName));
  }

  @DisplayName("HealthSocketServer.serverThreadName() returns empty when the server is not started")
  @Test
  @SuppressWarnings("resource")
  void serverThreadNameWhenNotStarted() {
    // ARRANGE
    var server = server();

    // ACT
    var threadName = server.serverThreadName(); // Get the name of the running server thread

    // ASSERT
    assertThat("The server thread name is empty", threadName, is(Optional.empty()));
  }

  @DisplayName("HealthSocketServer.serverThreadName() returns empty when the server is stopped")
  @Test
  @SneakyThrows
  @SuppressWarnings("resource")
  void serverThreadNameWhenStopped() {
    // ARRANGE
    var server = server();

    // ACT
    server
        .start() // Start the server
        .waitUntil(RUNNING) // Wait until the server is running
        .stop(); // Stop the server

    var threadName = server.serverThreadName(); // Get the name of the running server thread

    // ASSERT
    assertThat("The server thread name is empty", threadName, is(Optional.empty()));
  }

  @DisplayName("HealthSocketServer.interrupt() terminates gracefully")
  @Test
  @SneakyThrows
  void interrupt() {
    // ARRANGE
    var socketPath = socketPath();
    var socketPathname = socketPath.toString();
    var server = serverBuilder().socketPath(socketPath).build();
    var logAppender = attachLogAppenderTo(server);

    // ACT
    server
        .start() // Start the server
        .waitUntil(RUNNING) // Wait until the server is running
        .interrupt() // Interrupt the server thread
        .waitUntil(STOPPED); // Wait until the server is stopped

    logAppender.detach(); // Detach the log appender

    // ASSERT
    assertThat("The server is stopped", server.status(), is(STOPPED));

    assertThat(
        "The server logged the expected messages",
        logAppender,
        loggedInOrder(
            logEntry(
                INFO, "Health socket server listening (socketPathname=" + socketPathname + ")"),
            logEntry(DEBUG, "Health socket server thread interrupted"),
            logEntry(
                DEBUG,
                "Health socket server cleaned up the socket file (socketPathname="
                    + socketPathname
                    + ")"),
            logEntry(INFO, "Health socket server stopped")));
  }

  @DisplayName("HealthSocketServer.interrupt() executes safely when the server is not started")
  @Test
  @SuppressWarnings("resource")
  void interruptWhenNotStarted() {
    // ARRANGE
    var server = server();

    // ACT & ASSERT
    assertDoesNotThrow(server::interrupt); // Interrupt the server thread
  }

  @DisplayName(
      "HealthSocketServer.status() returns STOPPING when the server thread is not alive, but the still in the reference")
  @Test
  @SuppressWarnings({"unchecked", "resource"})
  void statusWhenServerThreadNotAlive() {
    // ARRANGE
    var serverThread = mock(Thread.class);
    when(serverThread.isAlive()).thenReturn(false);

    var serverThreadReference = mockServerThreadReference();
    when(serverThreadReference.get()).thenReturn(serverThread);

    var server =
        new HealthSocketServer(
            socketPath(), mockHealthEndpoint(), mockRuntime(), serverThreadReference);

    // ACT
    var status = server.status(); // Get the status of the server

    // ASSERT
    assertThat(status, is(STOPPING));
  }

  @DisplayName(
      "HealthSocketServer.waitUntil() throws an exception when the server does not reach the expected status within the timeout")
  @Test
  void waitUntilTimeout() {
    // ARRANGE
    var timeout = java.time.Duration.ofMillis(1);
    var server = server();
    var logAppender = attachLogAppenderTo(server);

    // ACT & ASSERT
    var exception =
        assertThrowsExactly(
            HealthSocketServer.ServerStatusTimeoutException.class,
            () -> server.waitUntil(RUNNING, timeout));

    assertThat(
        "The exception message is as expected",
        exception.getMessage(),
        is(
            "HealthSocketServer did not reach status RUNNING within 1 milliseconds, current status: STOPPED"));

    assertThat(
        "The server logged the expected messages",
        logAppender,
        loggedInOrder(
            logEntry(
                WARN,
                "HealthSocketServer did not reach status RUNNING within 1 milliseconds, current status: STOPPED")));
  }

  @DisplayName(
      "HealthSocketServer.stop() interrupts the server thread and waits for it to terminate gracefully")
  @Test
  @SneakyThrows
  void stop() {
    // ARRANGE
    var serverThread = mock(Thread.class);
    when(serverThread.isAlive()).thenReturn(true);

    var serverThreadReference = mockServerThreadReference();
    when(serverThreadReference.getAndSet(isNull())).thenReturn(serverThread);

    var server =
        new HealthSocketServer(
            socketPath(), mockHealthEndpoint(), mockRuntime(), serverThreadReference);

    // ACT
    server.close(); // Stop the server

    // ASSERT
    verify(serverThread, times(1)).interrupt();
    verify(serverThread, times(1)).join(any(Duration.class));
  }

  @DisplayName(
      "HealthSocketServer.stop() terminate gracefully even when the server thread is already terminated")
  @Test
  @SneakyThrows
  void stopWhenServerThreadIsNotAlive() {
    // ARRANGE
    var serverThreadName = "health-socket-server-test-thread-" + randomUUID();
    var serverThread = mock(Thread.class);
    when(serverThread.isAlive()).thenReturn(false);
    when(serverThread.getName()).thenReturn(serverThreadName);

    var serverThreadReference = mockServerThreadReference();
    when(serverThreadReference.getAndSet(isNull())).thenReturn(serverThread);

    var server =
        new HealthSocketServer(
            socketPath(), mockHealthEndpoint(), mockRuntime(), serverThreadReference);
    var logAppender = attachLogAppenderTo(server);

    // ACT
    server.close(); // Stop the server
    logAppender.detach(); // Detach the log appender

    // ASSERT
    assertThat(
        "The server logged the expected messages",
        logAppender,
        loggedInOrder(
            logEntry(
                DEBUG,
                "Health socket server thread is already terminated, no need to interrupt (threadName="
                    + serverThreadName
                    + ")")));
  }

  @DisplayName("HealthSocketServer.stop() logs a warning when the socket file deletion fails")
  @Test
  @SneakyThrows
  void stopWhenSocketFileDeletionFails() {
    // ARRANGE
    var socketPath = socketPath();
    var runtime = mockRuntime();
    doThrow(new IOException("Simulated socket file deletion failure"))
        .when(runtime)
        .deleteIfExists(socketPath);

    var server =
        new HealthSocketServer(
            socketPath, mockHealthEndpoint(), runtime, mockServerThreadReference());
    var logAppender = attachLogAppenderTo(server);

    // ACT
    server.close(); // Stop the server
    logAppender.detach(); // Detach the log appender

    // ASSERT
    assertThat(
        "The server logged the expected messages",
        logAppender,
        loggedInOrder(
            logEntry(
                WARN,
                "Health socket server socket file cleanup failed (socketPathname="
                    + socketPath
                    + ")")));
  }

  @DisplayName("HealthSocketServer server thread stops when socket channel creation fails")
  @Test
  @SneakyThrows
  void whenSocketChannelCreationFails() {
    // ARRANGE
    var runtime = mockRuntime();
    when(runtime.serverSocketChannel())
        .thenThrow(new IOException("Simulated socket channel creation failure"));

    var server =
        new HealthSocketServer(
            socketPath(), mockHealthEndpoint(), runtime, serverThreadReference());
    var logAppender = attachLogAppenderTo(server);

    // ACT
    server.start(); // Start the server
    logAppender.detach();

    // ASSERT
    assertThat("The server is stopped", server.status(), is(STOPPED));
    assertThat(
        "The server logged the expected messages",
        logAppender,
        loggedInOrder(
            logEntry(ERROR, "Health socket server threw an IOException"),
            logEntry(INFO, "Health socket server stopped")));
  }

  @DisplayName(
      "HealthSocketServer logs the first five socket channel write failures at debug level, and subsequent failures as warning")
  @Test
  @SneakyThrows
  @SuppressWarnings("java:S2925")
  void writeFailures() {
    // ARRANGE
    var server = server();
    var logAppender = attachLogAppenderTo(server);

    // ACT
    server
        .start() // Start the server
        .waitUntil(RUNNING); // Wait until the server is running

    readFrom(server.socketPath(), Duration.ofNanos(1)); // Read the response
    readFrom(server.socketPath(), Duration.ofNanos(1)); // Read the response
    readFrom(server.socketPath(), Duration.ofNanos(1)); // Read the response
    readFrom(server.socketPath(), Duration.ofNanos(1)); // Read the response
    readFrom(server.socketPath(), Duration.ofNanos(1)); // Read the response
    readFrom(server.socketPath(), Duration.ofNanos(1)); // Read the response
    readFrom(server.socketPath(), Duration.ofNanos(1)); // Read the response

    sleep(10);

    server
        .stop() // Stop the server
        .waitUntil(STOPPED); // Wait until the server is stopped
    logAppender.detach(); // Detach the log appender

    // ASSERT
    assertThat(
        "The server logged the expected messages",
        logAppender,
        loggedInOrder(
            logEntry(DEBUG, "Health socket server accepted a connection"),
            logEntry(DEBUG, "Failed to write health response to client (consecutive failures: 1)"),
            logEntry(DEBUG, "Health socket server accepted a connection"),
            logEntry(DEBUG, "Failed to write health response to client (consecutive failures: 2)"),
            logEntry(DEBUG, "Health socket server accepted a connection"),
            logEntry(DEBUG, "Failed to write health response to client (consecutive failures: 3)"),
            logEntry(DEBUG, "Health socket server accepted a connection"),
            logEntry(DEBUG, "Failed to write health response to client (consecutive failures: 4)"),
            logEntry(DEBUG, "Health socket server accepted a connection"),
            logEntry(DEBUG, "Failed to write health response to client (consecutive failures: 5)"),
            logEntry(DEBUG, "Health socket server accepted a connection"),
            logEntry(WARN, "Failed to write health response to client (consecutive failures: 6)"),
            logEntry(DEBUG, "Health socket server accepted a connection"),
            logEntry(WARN, "Failed to write health response to client (consecutive failures: 7)")));
  }

  private @NonNull HealthSocketServer server() {
    return serverBuilder().build();
  }

  private HealthSocketServer.HealthSocketServerBuilder serverBuilder() {
    return HealthSocketServer.builder()
        .socketPath(socketPath())
        .healthEndpoint(mockHealthEndpoint());
  }

  private @NonNull Path socketPath() {
    var socketPathname = "health-test-" + randomUUID() + ".sock";
    return Path.of(socketPathname);
  }

  private @NonNull HealthEndpoint mockHealthEndpoint() {
    var healthDescriptor = mockHealthDescriptor();
    return mockHealthEndpoint(healthDescriptor);
  }

  private @NonNull HealthEndpoint mockHealthEndpoint(HealthDescriptor healthDescriptor) {
    var healthEndpoint = mock(HealthEndpoint.class);
    when(healthEndpoint.health()).thenReturn(healthDescriptor);
    return healthEndpoint;
  }

  private @NonNull HealthDescriptor mockHealthDescriptor() {
    return mockHealthDescriptor(UP);
  }

  private @NonNull HealthDescriptor mockHealthDescriptor(Status actuatorStatus) {
    var healthDescriptor = mock(IndicatedHealthDescriptor.class);
    when(healthDescriptor.getStatus()).thenReturn(actuatorStatus);
    return healthDescriptor;
  }

  private HealthSocketServer.Runtime mockRuntime() {
    var runtime = mock(HealthSocketServer.Runtime.class);
    when(runtime.serverThreadFactory())
        .thenReturn(ofVirtual().name("health-socket-server-test-thread-" + randomUUID()).factory());
    return runtime;
  }

  private @NotNull AtomicReference<Thread> serverThreadReference() {
    return new AtomicReference<>();
  }

  @SuppressWarnings("unchecked")
  private @NonNull AtomicReference<Thread> mockServerThreadReference() {
    return (AtomicReference<Thread>) mock(AtomicReference.class);
  }

  private @NonNull TestLogAppender attachLogAppenderTo(HealthSocketServer server) {
    return TestLogAppender.attachTo(server.getClass());
  }
}
