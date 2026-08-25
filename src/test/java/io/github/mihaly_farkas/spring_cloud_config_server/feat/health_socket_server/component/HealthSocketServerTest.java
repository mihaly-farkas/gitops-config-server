package io.github.mihaly_farkas.spring_cloud_config_server.feat.health_socket_server.component;

import static ch.qos.logback.classic.Level.DEBUG;
import static ch.qos.logback.classic.Level.ERROR;
import static ch.qos.logback.classic.Level.INFO;
import static ch.qos.logback.classic.Level.WARN;
import static io.github.mihaly_farkas.spring_cloud_config_server.feat.health_socket_server.component.HealthSocketServer.HealthSocketServerBuilder;
import static io.github.mihaly_farkas.spring_cloud_config_server.feat.health_socket_server.component.HealthSocketServer.HealthSocketServerStatus.RUNNING;
import static io.github.mihaly_farkas.spring_cloud_config_server.feat.health_socket_server.component.HealthSocketServer.HealthSocketServerStatus.STOPPED;
import static io.github.mihaly_farkas.spring_cloud_config_server.feat.health_socket_server.component.HealthSocketServer.HealthSocketServerStatus.STOPPING;
import static io.github.mihaly_farkas.spring_cloud_config_server.feat.health_socket_server.component.HealthSocketServer.Runtime;
import static io.github.mihaly_farkas.spring_cloud_config_server.feat.health_socket_server.component.HealthSocketServer.ServerStatusTimeoutException;
import static io.github.mihaly_farkas.spring_cloud_config_server.lib.test_tool.MockitoHelper.assertVerify;
import static io.github.mihaly_farkas.spring_cloud_config_server.lib.test_tool.TestLogAppenderMatcher.logEntry;
import static io.github.mihaly_farkas.spring_cloud_config_server.lib.test_tool.TestLogAppenderMatcher.logged;
import static io.github.mihaly_farkas.spring_cloud_config_server.lib.test_tool.TestLogAppenderMatcher.loggedInOrder;
import static io.github.mihaly_farkas.spring_cloud_config_server.lib.test_tool.UnixSocketReader.readFrom;
import static java.lang.Thread.ofVirtual;
import static java.net.StandardProtocolFamily.UNIX;
import static java.nio.file.Files.deleteIfExists;
import static java.util.UUID.randomUUID;
import static java.util.stream.IntStream.range;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.health.contributor.Status.UP;

import io.github.mihaly_farkas.spring_cloud_config_server.feat.health_socket_server.component.HealthSocketServer.WorkerProcess;
import io.github.mihaly_farkas.spring_cloud_config_server.lib.test_tool.TestLogAppender;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
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
        Arguments.of(UP, 'H', "healthy"),
        Arguments.of(Status.DOWN, 'U', "unhealthy"),
        Arguments.of(Status.OUT_OF_SERVICE, 'U', "unhealthy"),
        Arguments.of(Status.UNKNOWN, 'U', "unhealthy"));
  }

  @DisplayName("HealthSocketServer cannot be created with a null socket path")
  @Test
  void nullSocketPath() {
    // ARRANGE
    var serverBuilder = serverBuilder().socketPath(null);

    // ACT
    var exception = assertThrowsExactly(IllegalArgumentException.class, serverBuilder::build);

    // ASSERT
    assertThat(
        "The exception message is as expected",
        exception.getMessage(),
        is("socketPath must not be null"));
  }

  @DisplayName("HealthSocketServer cannot be created with a null health endpoint")
  @Test
  void nullHealthEndpoint() {
    // ARRANGE
    var serverBuilder = serverBuilder().healthEndpoint(null);

    // ACT
    var exception = assertThrowsExactly(IllegalArgumentException.class, serverBuilder::build);

    // ASSERT
    assertThat(
        "The exception message is as expected",
        exception.getMessage(),
        is("healthEndpoint must not be null"));
  }

  @DisplayName("HealthSocketServer is an auto-closeable resource")
  @Test
  @SneakyThrows
  void autoCloseable() {
    // ARRANGE
    var server = server();

    // ACT
    try (server) {
      server.start().waitUntil(RUNNING);
    }

    // ASSERT
    assertThat("The server is stopped", server.getStatus(), is(STOPPED));
  }

  @DisplayName("HealthSocketServer responses via socket based on Actuator Health Endpoint status")
  @MethodSource("healthStatuses")
  @ParameterizedTest(name = "actuatorStatus: {0}")
  @SneakyThrows
  void socketResponse(Status actuatorStatus, char expectedStatus, String expectedStatusText) {
    // ARRANGE
    var expectedLogMessage = "Response sent: '" + expectedStatus + "' (" + expectedStatusText + ")";
    var socketPath = socketPath();
    var healthDescriptor = mockHealthDescriptor(actuatorStatus);
    var healthEndpoint = mockHealthEndpoint(healthDescriptor);
    var server = serverBuilder().socketPath(socketPath).healthEndpoint(healthEndpoint).build();
    var logAppender = attachLogAppenderTo(server);
    server.start().waitUntil(RUNNING);

    // ACT
    var status = readFrom(socketPath);
    logAppender.flushLogs();

    // ASSERT
    assertThat("The response is not empty", status.isPresent(), is(true));
    assertThat("The response is '" + expectedStatus + "'", status.get(), is(expectedStatus));
    assertThat(
        "Logged messages are:",
        logAppender,
        loggedInOrder(logEntry(DEBUG, "Client connected"), logEntry(DEBUG, expectedLogMessage)));

    // CLEANUP
    server.close();
    logAppender.detach();
  }

  @DisplayName("HealthSocketServer.start() executes safely when called twice")
  @Test
  @SneakyThrows
  void startTwice() {
    // ARRANGE
    var server = server();
    var logAppender = attachLogAppenderTo(server);
    server.start().waitUntil(RUNNING);
    var workerThreadName = server.getWorkerThreadName().orElseThrow();

    // ACT
    server.start();

    // ASSERT
    assertThat(
        "Logged messages are:",
        logAppender,
        logged(WARN, "Already running, worker thread name: " + workerThreadName));

    // CLEANUP
    server.close();
    logAppender.detach();
  }

  @DisplayName("HealthSocketServer.start() executes safely when called concurrently")
  @Test
  @SneakyThrows
  void startConcurrently() {
    // ARRANGE
    var workerThreadName = "health-socket-server-test-thread-" + randomUUID();
    var workerThreadSetByOtherName =
        "health-socket-server-test-thread-set-by-other-call-" + randomUUID();
    var workerThread = mock(Thread.class);
    var workerThreadSetByOther = mock(Thread.class);
    var workerThreadSetByOtherNameOptional = Optional.of(workerThreadSetByOtherName);
    var workerThreadReference = mockWorkerThreadReference();
    var workerThreadFactory = mock(ThreadFactory.class);
    var runtime = mock(Runtime.class);
    var server =
        new HealthSocketServer(
            mock(Path.class), mock(HealthEndpoint.class), runtime, workerThreadReference);
    var logAppender = attachLogAppenderTo(server);

    // Mock the AtomicReference holding the running server thread.
    //
    // Simulates a race condition where two threads call start() concurrently:
    // 1. The slower thread checks if the reference is null.
    // 2. A faster thread sets the reference in the meantime.
    // 3. The slower thread finds a non-null reference immediately after its check.
    when(workerThread.getName()).thenReturn(workerThreadName);
    when(workerThreadSetByOther.getName()).thenReturn(workerThreadSetByOtherName);
    when(workerThreadReference.get()).thenReturn(null, workerThreadSetByOther);
    when(workerThreadReference.compareAndSet(null, workerThread)).thenReturn(false);
    when(runtime.workerThreadFactory()).thenReturn(workerThreadFactory);
    when(workerThreadFactory.newThread(any(HealthSocketServer.WorkerProcess.class)))
        .thenReturn(workerThread);

    // ACT
    server.start();

    // ASSERT
    assertThat(
        "The server thread is the one which created by the faster starter",
        server.getWorkerThreadName(),
        is(workerThreadSetByOtherNameOptional));
    assertVerify(
        "The server thread that was created by the slower starter is never started",
        () -> verify(workerThread, times(0)).start());
    assertThat(
        "Logged messages are:",
        logAppender,
        logged(WARN, "Already running, worker thread name: " + workerThreadSetByOtherName));

    // CLEANUP
    server.close();
    logAppender.detach();
  }

  @DisplayName("HealthSocketServer.workerThreadName() returns the name of the worker thread")
  @Test
  @SneakyThrows
  void workerThreadName() {
    // ARRANGE
    var workerThreadName = Optional.of("health-socket-server-test-thread-" + randomUUID());
    var virtualThreadBuilder = ofVirtual().name(workerThreadName.get());
    var server = serverBuilder().virtualThreadBuilder(virtualThreadBuilder).build();
    server.start().waitUntil(RUNNING);

    // ACT
    var threadName = server.getWorkerThreadName();

    // ASSERT
    assertThat(
        "The server thread name is the one that was set for the thread builder during creation",
        threadName,
        is(workerThreadName));

    // CLEANUP
    server.close();
  }

  @DisplayName("HealthSocketServer.workerThreadName() returns empty when the server is not started")
  @Test
  void workerThreadNameWhenNotStarted() {
    // ARRANGE
    var server = server();

    // ACT
    var threadName = server.getWorkerThreadName();

    // ASSERT
    assertThat("The server thread name is empty", threadName, is(Optional.empty()));

    // CLEANUP
    server.close();
  }

  @DisplayName("HealthSocketServer.workerThreadName() returns empty when the server is stopped")
  @Test
  @SneakyThrows
  void workerThreadNameWhenStopped() {
    // ARRANGE
    var server = server();
    server.start().waitUntil(RUNNING).close();

    // ACT
    var threadName = server.getWorkerThreadName();

    // ASSERT
    assertThat("The server thread name is empty", threadName, is(Optional.empty()));
  }

  @DisplayName(
      "HealthSocketServer.status() returns STOPPING"
          + " when the server thread is not alive, but the still in the reference")
  @Test
  void statusWhenWorkerThreadNotAlive() {
    // ARRANGE
    var workerThread = mock(Thread.class);
    var workerThreadReference = mockWorkerThreadReference();
    var server =
        new HealthSocketServer(
            socketPath(), mockHealthEndpoint(), mockRuntime(), workerThreadReference);

    when(workerThread.isAlive()).thenReturn(false);
    when(workerThreadReference.get()).thenReturn(workerThread);

    // ACT
    var status = server.getStatus(); // Get the status of the server

    // ASSERT
    assertThat(status, is(STOPPING));

    // CLEANUP
    server.close();
  }

  @DisplayName(
      "HealthSocketServer.waitUntil() throws an exception"
          + " when the server does not reach the expected status within the timeout")
  @Test
  void waitUntilTimeout() {
    // ARRANGE
    var timeout = java.time.Duration.ofMillis(1);
    var server = server();

    // ACT
    var exception =
        assertThrowsExactly(
            ServerStatusTimeoutException.class, () -> server.waitUntil(RUNNING, timeout));

    // ASSERT
    assertThat(
        "The exception message is as expected",
        exception.getMessage(),
        is("Did not reach status RUNNING within 1 milliseconds, current status: STOPPED"));

    // CLEANUP
    server.close();
  }

  @DisplayName(
      "HealthSocketServer.close() interrupts the worker thread"
          + " and waits for it to terminate gracefully")
  @Test
  void close() {
    // ARRANGE
    var workerThread = mock(Thread.class);
    var workerThreadReference = mockWorkerThreadReference();
    var workerThreadFactory = mock(ThreadFactory.class);
    var runtime = mockRuntime();
    var server =
        new HealthSocketServer(socketPath(), mockHealthEndpoint(), runtime, workerThreadReference);

    when(workerThreadReference.get()).thenReturn(null, workerThread);
    when(runtime.workerThreadFactory()).thenReturn(workerThreadFactory);
    when(workerThreadFactory.newThread(any(WorkerProcess.class))).thenReturn(workerThread);
    when(workerThreadReference.compareAndSet(null, workerThread)).thenReturn(true);
    doAnswer(
            _ -> {
              server.socketReady.set(true);
              return null;
            })
        .when(workerThread)
        .start();
    when(workerThread.isAlive()).thenReturn(true);
    when(workerThread.isInterrupted()).thenReturn(true);

    server.start().close();

    // ACT
    var status = server.getStatus();

    // ASSERT
    assertThat("The server status in STOPPING", status, is(STOPPING));
    assertVerify(
        "The worker thread is interrupted", () -> verify(workerThread, times(1)).interrupt());
    assertVerify(
        "The worker thread is joined with a timeout",
        () -> verify(workerThread, times(1)).join(any(Duration.class)));
    assertVerify(
        "The worker thread reference is not cleared (the worker thread should do that)",
        () -> verify(workerThreadReference, times(0)).compareAndSet(workerThread, null));
  }

  @DisplayName(
      "HealthSocketServer.close() interrupts the worker thread"
          + " and removes the reference"
          + " when the thread does not terminate gracefully within the timeout")
  @Test
  @SneakyThrows
  void closeWhenWorkerThreadDoesNotTerminateGracefully() {
    // ARRANGE
    var workerThread = mock(Thread.class);
    var workerThreadReference = mockWorkerThreadReference();
    var workerThreadFactory = mock(ThreadFactory.class);
    var runtime = mockRuntime();
    var server =
        new HealthSocketServer(socketPath(), mockHealthEndpoint(), runtime, workerThreadReference);
    var logAppender = attachLogAppenderTo(server);

    when(workerThreadReference.get()).thenReturn(null, workerThread, null);
    when(runtime.workerThreadFactory()).thenReturn(workerThreadFactory);
    when(workerThreadFactory.newThread(any(WorkerProcess.class))).thenReturn(workerThread);
    when(workerThreadReference.compareAndSet(null, workerThread)).thenReturn(true);
    doAnswer(
            _ -> {
              server.socketReady.set(true);
              return null;
            })
        .when(workerThread)
        .start();
    when(workerThread.isAlive()).thenReturn(true);
    when(workerThread.join(any(Duration.class)))
        .thenThrow(new InterruptedException("Simulated join timeout"));

    server.start().close();

    // ACT
    var status = server.getStatus();

    // ASSERT
    assertThat("The server status in STOPPED", status, is(STOPPED));
    assertVerify(
        "The worker thread is interrupted"
            + " and then interrupted again when the exception is caught",
        () -> verify(workerThread, times(2)).interrupt());
    assertVerify(
        "The worker thread is joined with a timeout",
        () -> verify(workerThread, times(1)).join(any(Duration.class)));
    assertVerify(
        "The worker thread reference is cleared (force removed by the server)",
        () -> verify(workerThreadReference, times(1)).compareAndSet(workerThread, null));

    assertThat("Logged messages are:", logAppender, logged(WARN, "Exception occurred"));

    // CLEANUP
    logAppender.detach();
  }

  @DisplayName(
      "HealthSocketServer.close() terminate gracefully"
          + " even when the server thread is already terminated")
  @Test
  @SneakyThrows
  void closeWhenWorkerThreadIsNotAlive() {
    // ARRANGE
    var workerThreadName = "health-socket-server-test-thread-" + randomUUID();
    var workerThread = mock(Thread.class);
    var workerThreadReference = mockWorkerThreadReference();
    var server =
        new HealthSocketServer(
            socketPath(), mockHealthEndpoint(), mockRuntime(), workerThreadReference);
    var logAppender = attachLogAppenderTo(server);

    when(workerThread.isAlive()).thenReturn(false);
    when(workerThread.getName()).thenReturn(workerThreadName);
    when(workerThreadReference.get()).thenReturn(workerThread);
    when(workerThreadReference.compareAndSet(workerThread, null)).thenReturn(true);

    // ACT
    server.close();

    // ASSERT
    assertThat(
        "Logged messages are:",
        logAppender,
        loggedInOrder(logEntry(DEBUG, "Worker thread is already terminated: " + workerThreadName)));

    // CLEANUP
    logAppender.detach();
  }

  @DisplayName("HealthSocketServer.close() logs a warning when the socket file deletion fails")
  @Test
  @SneakyThrows
  void closeWhenSocketFileDeletionFails() {
    // ARRANGE
    var socketPath = socketPath();
    var runtime = mockRuntime();
    var server =
        new HealthSocketServer(socketPath, mockHealthEndpoint(), runtime, workerThreadReference());
    var logAppender = attachLogAppenderTo(server);

    doThrow(new IOException("Simulated socket file deletion failure"))
        .when(runtime)
        .deleteIfExists(socketPath);

    server.start().waitUntil(RUNNING);

    // ACT
    server.close();

    // ASSERT
    assertThat(
        "Logged messages are:",
        logAppender,
        loggedInOrder(logEntry(WARN, "Socket file deletion failed: " + socketPath)));

    // CLEANUP
    logAppender.detach();
    deleteIfExists(socketPath);
  }

  @DisplayName("HealthSocketServer server thread stops when socket channel creation fails")
  @Test
  @SneakyThrows
  void whenSocketChannelCreationFails() {
    // ARRANGE
    var runtime = mockRuntime();
    var server =
        new HealthSocketServer(
            socketPath(), mockHealthEndpoint(), runtime, workerThreadReference());
    var logAppender = attachLogAppenderTo(server);

    when(runtime.serverSocketChannel())
        .thenThrow(new IOException("Simulated socket channel creation failure"));

    server.start();

    // ACT
    server.waitUntil(STOPPED);

    // ASSERT
    assertThat(
        "Logged messages are:",
        logAppender,
        loggedInOrder(
            logEntry(ERROR, "Exception occurred"),
            logEntry(DEBUG, "Interrupted"),
            logEntry(DEBUG, "Socket file does not exist: " + server.getSocketPath()),
            logEntry(INFO, "Stopped")));

    // CLEANUP
    logAppender.detach();
  }

  @DisplayName(
      "HealthSocketServer logs the first five socket channel write failures at debug level,"
          + " and subsequent failures as warning")
  @Test
  @SneakyThrows
  @SuppressWarnings("java:S2925")
  void writeFailures() {
    // ARRANGE
    var server = server();
    var logAppender = attachLogAppenderTo(server);
    server.start().waitUntil(RUNNING);

    // ACT
    range(0, 7).forEach(_ -> readFrom(server.getSocketPath(), Duration.ofNanos(1)));
    logAppender.flushLogs();

    // ASSERT
    assertThat(
        "Logged messages are:",
        logAppender,
        loggedInOrder(
            logEntry(DEBUG, "Client connected"),
            logEntry(DEBUG, "Failed to write response, consecutive failures: 1"),
            logEntry(DEBUG, "Client connected"),
            logEntry(DEBUG, "Failed to write response, consecutive failures: 2"),
            logEntry(DEBUG, "Client connected"),
            logEntry(DEBUG, "Failed to write response, consecutive failures: 3"),
            logEntry(DEBUG, "Client connected"),
            logEntry(DEBUG, "Failed to write response, consecutive failures: 4"),
            logEntry(DEBUG, "Client connected"),
            logEntry(DEBUG, "Failed to write response, consecutive failures: 5"),
            logEntry(DEBUG, "Client connected"),
            logEntry(WARN, "Failed to write response, consecutive failures: 6"),
            logEntry(DEBUG, "Client connected"),
            logEntry(WARN, "Failed to write response, consecutive failures: 7")));

    // CLEANUP
    server.close();
    logAppender.detach();
  }

  private @NonNull HealthSocketServer server() {
    return serverBuilder().build();
  }

  private HealthSocketServerBuilder serverBuilder() {
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

  @SneakyThrows
  private Runtime mockRuntime() {
    var runtime = mock(Runtime.class);
    when(runtime.workerThreadFactory())
        .thenReturn(ofVirtual().name("health-socket-server-test-thread-" + randomUUID()).factory());
    when(runtime.serverSocketChannel()).thenReturn(ServerSocketChannel.open(UNIX));
    when(runtime.deleteIfExists(any(Path.class)))
        .thenAnswer(
            invocation -> {
              Path path = invocation.getArgument(0);
              return deleteIfExists(path);
            });
    return runtime;
  }

  private @NotNull AtomicReference<Thread> workerThreadReference() {
    return new AtomicReference<>();
  }

  @SuppressWarnings("unchecked")
  private @NonNull AtomicReference<Thread> mockWorkerThreadReference() {
    return (AtomicReference<Thread>) mock(AtomicReference.class);
  }

  private @NonNull TestLogAppender attachLogAppenderTo(HealthSocketServer server) {
    return TestLogAppender.attachTo(server.getClass());
  }
}
