package io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component;

import static io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component.HealthSocketServer.DefaultRuntime;
import static io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component.HealthSocketServer.Status.RUNNING;
import static io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component.HealthSocketServer.Status.STARTING;
import static io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component.HealthSocketServer.Status.STOPPED;
import static io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component.HealthSocketServer.Status.STOPPING;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.health.contributor.Status.UP;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.boot.health.actuate.endpoint.HealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.actuate.endpoint.IndicatedHealthDescriptor;

@SuppressWarnings("SameParameterValue")
class HealthSocketServerTest {

  static final char HEALTHY = 'H';

  static final Duration SOCKET_READ_DEFAULT_TIMEOUT = Duration.ofSeconds(1);

  static TestServerBuilder builder() {
    return new TestServerBuilder();
  }

  @DisplayName("HealthSocketServer.start() executes safely when called twice")
  @Test
  @SneakyThrows
  void startTwice() {
    // ARRANGE
    var healthSocketServer =
        builder()
            .and(server -> when(server.healthDescriptor().getStatus()).thenReturn(UP))
            .build()
            .start()
            .waitUntil(RUNNING)
            .start();

    // ACT
    char response = readSingleByteWithTimeout(healthSocketServer.socketPath());

    // ASSERT
    assertThat(response, is(HEALTHY));

    // CLEANUP
    healthSocketServer.close();
  }

  @DisplayName("HealthSocketServer.start() executes safely when called concurrently")
  @Test
  @SneakyThrows
  @SuppressWarnings("resource")
  void startConcurrently() {
    // ARRANGE
    var serverThread = mock(Thread.class);
    var serverThreadSetByOtherCall = mock(Thread.class);

    when(serverThread.getName()).thenReturn("HealthSocketServer-ServerThread");
    when(serverThreadSetByOtherCall.getName()).thenReturn("HealthSocketServer-ServerThread-Other");

    var healthSocketServer =
        builder()
            .mockRuntime()
            .serverThreadReference(mock(AtomicReference.class))
            .and(
                server ->
                    when(server.serverThreadReference.get())
                        .thenReturn(null, serverThreadSetByOtherCall))
            .and(
                server ->
                    when(server
                            .runtime()
                            .serverThreadFactory()
                            .newThread(any(HealthSocketServer.ServerProcess.class)))
                        .thenReturn(serverThread))
            .and(
                server ->
                    when(server.serverThreadReference.compareAndSet(null, serverThread))
                        .thenReturn(false))
            .build();

    // ACT
    healthSocketServer.start();

    // ASSERT
    // The created server thread should not be started because another thread has already set the
    // reference
    verify(serverThread, times(0)).start();
  }

  @DisplayName(
      "HealthSocketServer.serverThreadName() returns the name of the running server thread")
  @Test
  void serverThreadName() {
    // ARRANGE
    var serverThreadName = "health-socket-server-test-thread-" + randomUUID();

    var healthSocketServer = builder().serverThreadName(serverThreadName).build().start();

    // ACT
    var threadName = healthSocketServer.serverThreadName();

    // ASSERT
    assertThat(threadName, is(Optional.of(serverThreadName)));

    // CLEANUP
    healthSocketServer.close();
  }

  @DisplayName("HealthSocketServer.serverThreadName() returns empty when the server is not started")
  @Test
  @SuppressWarnings("resource")
  void serverThreadNameWhenNotStarted() {
    // ARRANGE
    var healthSocketServer = builder().build();

    // ACT
    var threadName = healthSocketServer.serverThreadName();

    // ASSERT
    assertThat(threadName, is(Optional.empty()));
  }

  @DisplayName(
      "HealthSocketServer.interrupt() terminates gracefully when called before the server is started")
  @Test
  @SuppressWarnings("resource")
  void interruptBeforeStart() {
    // ARRANGE
    var healthSocketServer = builder().build();

    // ACT
    healthSocketServer.interrupt();

    // ASSERT
    assertThat(healthSocketServer.waitUntil(STOPPED).status(), is(STOPPED));
  }

  @DisplayName(
      "HealthSocketServer.interrupt() terminates gracefully when called after the server is closed")
  @Test
  void interruptAfterStop() {
    // ARRANGE
    var healthSocketServer = builder().build().waitUntil(STOPPED).start().waitUntil(RUNNING);
    healthSocketServer.close();

    // ACT
    healthSocketServer.interrupt();

    // ASSERT
    assertThat(healthSocketServer.waitUntil(STOPPED).status(), is(STOPPED));
  }

  @DisplayName(
      "HealthSocketServer.close() executes safely when server thread is already terminated")
  @Test
  @SneakyThrows
  void closeWhenServerThreadAlreadyTerminated() {
    // ARRANGE
    var serverThread = mock(Thread.class);
    when(serverThread.isAlive()).thenReturn(false);

    var healthSocketServer =
        builder()
            .serverThreadReference(mock(AtomicReference.class))
            .and(
                server ->
                    when(server.serverThreadReference.getAndSet(isNull())).thenReturn(serverThread))
            .build();
    healthSocketServer.close();

    // ACT
    var status = healthSocketServer.status();

    // ASSERT
    assertThat(status, is(STOPPED));
  }

  @DisplayName("HealthSocketServer.close() executes safely when socket file deletion fails")
  @Test
  @SneakyThrows
  void closeWhenSocketFileDeletionFails() {
    // ARRANGE
    var serverThread = mock(Thread.class);

    var healthSocketServer =
        builder()
            .serverThreadReference(mock(AtomicReference.class))
            .mockRuntime()
            .and(
                server ->
                    when(server.serverThreadReference.getAndSet(isNull())).thenReturn(serverThread))
            .andDo(
                server ->
                    doThrow(new IOException("Deletion failed"))
                        .when(server.runtime())
                        .deleteIfExists(server.socketPath))
            .build();
    healthSocketServer.close();

    // ACT
    var status = healthSocketServer.status();

    // ASSERT
    assertThat(status, is(STOPPED));
  }

  @DisplayName(
      "HealthSocketServer.status() returns the correct status when the server is not started")
  @Test
  void statusWhenNotStarted() {
    // ARRANGE
    var healthSocketServer = builder().build();

    // ACT
    var status = healthSocketServer.status();

    // ASSERT
    assertThat(status, is(STOPPED));

    // CLEANUP
    healthSocketServer.close();
  }

  @DisplayName("HealthSocketServer.status() returns the correct status when the server is started")
  @Test
  void statusWhenStarted() {
    // ARRANGE
    var healthSocketServer =
        builder().build().waitUntil(STOPPED).start().waitUntil(STARTING).waitUntil(RUNNING);

    // ACT
    var status = healthSocketServer.status();

    // ASSERT
    assertThat(status, is(RUNNING));

    // CLEANUP
    healthSocketServer.close();
  }

  @DisplayName(
      "HealthSocketServer.status() returns the correct status when the server is interrupted")
  @Test
  void statusWhenInterrupted() {
    // ARRANGE
    var healthSocketServer =
        builder()
            .build()
            .waitUntil(STOPPED)
            .start()
            .waitUntil(RUNNING)
            .interrupt()
            .waitUntil(STOPPING)
            .waitUntil(STOPPED);

    // ACT
    var status = healthSocketServer.status();

    // ASSERT
    assertThat(status, is(STOPPED));

    // CLEANUP
    healthSocketServer.close();
  }

  @DisplayName(
      "HealthSocketServer.status() returns the correct status when the server thread is stopped, but the server is not closed yet")
  @Test
  @SneakyThrows
  void statusWhenThreadStoppedButNotClosed() {
    // ARRANGE
    var serverThread = mock(Thread.class);

    when(serverThread.isAlive()).thenReturn(false);

    var healthSocketServer =
        builder()
            .serverThreadReference(mock(AtomicReference.class))
            .and(server -> when(server.serverThreadReference.get()).thenReturn(serverThread))
            .build();

    // ACT
    var status = healthSocketServer.status();

    // ASSERT
    assertThat(status, is(STOPPING));

    // CLEANUP
    healthSocketServer.close();
  }

  @DisplayName("HealthSocketServer.status() returns the correct status when the server is closed")
  @Test
  void statusWhenClosed() {
    // ARRANGE
    var healthSocketServer =
        builder().build().waitUntil(STOPPED).start().waitUntil(STARTING).waitUntil(RUNNING);
    healthSocketServer.close();

    // ACT
    var status = healthSocketServer.status();

    // ASSERT
    assertThat(status, is(STOPPED));
  }

  @DisplayName(
      "HealthSocketServer.waitUntil() throws an exception when the server does not reach the expected status within the timeout")
  @Test
  @SuppressWarnings("resource")
  void waitUntilTimeout() {
    // ARRANGE
    var timeout = Duration.ofMillis(1);
    var healthSocketServer = builder().build();

    // ACT & ASSERT
    assertThrows(
        HealthSocketServer.ServerStatusTimeoutException.class,
        () -> healthSocketServer.waitUntil(RUNNING, timeout));
  }

  @DisplayName(
      "HealthSocketServer.close() shuts down gracefully when the server thread is not terminated in time after being interrupted")
  @Test
  @SneakyThrows
  void closeWhenServerThreadNotTerminated() {
    // ARRANGE
    var serverThread = mock(Thread.class);

    when(serverThread.isAlive()).thenReturn(true);

    var healthSocketServer =
        builder()
            .serverThreadReference(mock(AtomicReference.class))
            .and(
                server ->
                    when(server.serverThreadReference.getAndSet(null)).thenReturn(serverThread))
            .build();

    // ACT
    healthSocketServer.close();

    // ASSERT
    verify(serverThread, times(1)).interrupt();
    verify(serverThread, times(1)).join(Duration.ofMillis(3000));
  }

  @DisplayName("HealthSocketServer stops gracefully when server socket creation fails")
  @Test
  @SneakyThrows
  @SuppressWarnings("resource")
  void whenClosedByInterruptException() {
    // ARRANGE
    var healthSocketServer =
        builder()
            .mockRuntime()
            .and(
                server ->
                    when(server.runtime().serverThreadFactory())
                        .thenReturn(
                            Thread.ofVirtual()
                                .name("health-socket-server-test-thread-" + server.testId())
                                .factory()))
            .and(
                server ->
                    when(server.runtime().serverSocketChannel())
                        .thenThrow(
                            new IOException(
                                "Simulated socket creation failure", new IOException())))
            .build();

    // ACT
    healthSocketServer.start();

    // ASSERT
    assertThat(healthSocketServer.waitUntil(STOPPED).status(), is(STOPPED));
  }

  @DisplayName("HealthSocketServer stops gracefully when server socket binding fails")
  @Test
  @SneakyThrows
  @SuppressWarnings("resource")
  void whenClosedByBindException() {
    // ARRANGE
    var serverSocket = mock(ServerSocketChannel.class);

    var healthSocketServer =
        builder()
            .mockRuntime()
            .and(
                server ->
                    when(server.runtime().serverThreadFactory())
                        .thenReturn(
                            Thread.ofVirtual()
                                .name("health-socket-server-test-thread-" + server.testId())
                                .factory()))
            .and(server -> when(server.runtime().serverSocketChannel()).thenReturn(serverSocket))
            .andDo(
                _ ->
                    doThrow(new IOException("Simulated socket binding failure"))
                        .when(serverSocket)
                        .bind(any(SocketAddress.class)))
            .build();

    // ACT
    healthSocketServer.start();

    // ASSERT
    assertThat(healthSocketServer.waitUntil(STOPPED).status(), is(STOPPED));
  }

  @DisplayName("HealthSocketServer stops gracefully when accepting a connection fails")
  @Test
  @SneakyThrows
  @SuppressWarnings("resource")
  void whenAcceptFails() {
    // ARRANGE
    var serverSocket = mock(ServerSocketChannel.class);

    var healthSocketServer =
        builder()
            .mockRuntime()
            .and(
                server ->
                    when(server.runtime().serverThreadFactory())
                        .thenReturn(
                            Thread.ofVirtual()
                                .name("health-socket-server-test-thread-" + server.testId())
                                .factory()))
            .and(server -> when(server.runtime().serverSocketChannel()).thenReturn(serverSocket))
            .and(_ -> when(serverSocket.bind(any(SocketAddress.class))).thenReturn(serverSocket))
            .andDo(_ -> doThrow(new ClosedByInterruptException()).when(serverSocket).accept())
            .build();

    // ACT
    healthSocketServer.start();

    // ASSERT
    assertThat(healthSocketServer.waitUntil(STOPPED).status(), is(STOPPED));
  }

  @SneakyThrows
  char readSingleByteWithTimeout(Path socketPath) {
    return readSingleByteWithTimeout(socketPath, SOCKET_READ_DEFAULT_TIMEOUT);
  }

  @SneakyThrows
  char readSingleByteWithTimeout(Path socketPath, Duration timeout) {
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
          return (char) buffer.get();
        }
      }
    }

    throw new RuntimeException("Timeout while waiting for a byte to be read from the socket");
  }

  static class TestServerBuilder {
    private final String tempDir = System.getProperty("java.io.tmpdir");
    private final String testId = randomUUID().toString();
    private final HealthDescriptor healthDescriptor = mock(IndicatedHealthDescriptor.class);
    private final ThreadFactory serverThreadFactory = mock(ThreadFactory.class);
    private final ServerSocketChannel serverSocketChannel = mock(ServerSocketChannel.class);
    private String socketPathname = tempDir + "/health-test-" + testId + ".sock";
    private String serverThreadName = "health-socket-server-test-thread-" + testId;
    private Path socketPath = Path.of(socketPathname);
    private HealthEndpoint healthEndpoint = mock(HealthEndpoint.class);
    private HealthSocketServer.Runtime runtime;
    private AtomicReference<Thread> serverThreadReference = new AtomicReference<>();

    TestServerBuilder() {
      when(healthEndpoint.health()).thenReturn(healthDescriptor);
    }

    TestServerBuilder socketPathname(String socketPathname) {
      this.socketPathname = socketPathname;
      this.socketPath = Path.of(socketPathname);
      return this;
    }

    TestServerBuilder socketPath(Path socketPath) {
      this.socketPath = socketPath;
      return this;
    }

    TestServerBuilder serverThreadName(String serverThreadName) {
      this.serverThreadName = serverThreadName;
      return this;
    }

    TestServerBuilder healthEndpoint(HealthEndpoint healthEndpoint) {
      this.healthEndpoint = healthEndpoint;
      return this;
    }

    TestServerBuilder runtime(HealthSocketServer.Runtime runtime) {
      this.runtime = runtime;
      return this;
    }

    @SuppressWarnings("unchecked")
    TestServerBuilder serverThreadReference(AtomicReference<?> serverThreadReference) {
      this.serverThreadReference = (AtomicReference<Thread>) serverThreadReference;
      return this;
    }

    @SneakyThrows
    TestServerBuilder mockRuntime() {
      this.runtime = mock(HealthSocketServer.Runtime.class);
      when(runtime.serverThreadFactory()).thenReturn(serverThreadFactory);
      when(runtime.serverSocketChannel()).thenReturn(serverSocketChannel);
      return this;
    }

    <R> TestServerBuilder and(
        ExceptionThrowingFunction<TestServerBuilder, OngoingStubbing<?>> callback)
        throws Exception {
      callback.apply(this);
      return this;
    }

    TestServerBuilder andDo(ExceptionThrowingConsumer<TestServerBuilder> callback)
        throws Exception {
      callback.accept(this);
      return this;
    }

    public String testId() {
      return testId;
    }

    HealthDescriptor healthDescriptor() {
      return healthDescriptor;
    }

    HealthEndpoint healthEndpoint() {
      return healthEndpoint;
    }

    AtomicReference<Thread> serverThreadReference() {
      return serverThreadReference;
    }

    HealthSocketServer.Runtime runtime() {
      return runtime;
    }

    HealthSocketServer build() {
      if (runtime == null) {
        runtime = new DefaultRuntime(Thread.ofVirtual().name(serverThreadName));
      }

      return new HealthSocketServer(socketPath, healthEndpoint, runtime, serverThreadReference);
    }

    @FunctionalInterface
    interface ExceptionThrowingFunction<T, R> {
      R apply(T t) throws Exception;
    }

    @FunctionalInterface
    interface ExceptionThrowingConsumer<T> {
      void accept(T t) throws Exception;
    }
  }
}
