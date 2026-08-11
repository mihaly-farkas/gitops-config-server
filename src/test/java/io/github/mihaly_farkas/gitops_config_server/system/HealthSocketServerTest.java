package io.github.mihaly_farkas.gitops_config_server.system;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.concurrent.atomic.AtomicReference;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.actuate.endpoint.IndicatedHealthDescriptor;
import org.springframework.boot.health.contributor.Status;

class HealthSocketServerTest {

  @Test
  @DisplayName("HealthSocketServer starts and stops without error")
  void startAndStop() {
    // ARRANGE
    var timestamp = System.currentTimeMillis();
    var socketPath = "test." + timestamp + ".sock";
    var serverThread = new AtomicReference<Thread>(null);

    var healthSocketServer = new HealthSocketServer(socketPath, null, serverThread, null);

    // ACT & ASSERT
    assertDoesNotThrow(healthSocketServer::start);
    assertDoesNotThrow(healthSocketServer::stop);
  }

  @Test
  @DisplayName("HealthSocketServer starts and stops multiple times without error")
  void startAndMultipleStops() {
    // ARRANGE
    var timestamp = System.currentTimeMillis();
    var socketPath = "test." + timestamp + ".sock";
    var serverThread = new AtomicReference<Thread>(null);

    var healthSocketServer = new HealthSocketServer(socketPath, null, serverThread, null);

    // ACT & ASSERT
    assertDoesNotThrow(healthSocketServer::start);
    assertDoesNotThrow(healthSocketServer::stop);
    assertDoesNotThrow(healthSocketServer::stop);
    assertDoesNotThrow(healthSocketServer::stop);
  }

  @Test
  @DisplayName(
      "HealthSocketServer throws RuntimeException in the virtual thread when cannot create socket")
  @SneakyThrows
  void startWithSocketCreationFailure() {
    // ARRANGE
    var socketPath = "/invalid/path/to/socket.sock";
    var serverThread = new AtomicReference<Thread>(null);
    var caughtException = new AtomicReference<Throwable>();

    // Condition removed to prevent a race condition if the thread fails before serverThread is set
    var customHandler =
        (Thread.UncaughtExceptionHandler) (_, throwable) -> caughtException.set(throwable);

    var healthSocketServer = new HealthSocketServer(socketPath, null, serverThread, customHandler);

    // ACT & ASSERT
    assertDoesNotThrow(healthSocketServer::start);

    // ARRANGE
    var virtualThread = serverThread.get();

    // ACT & ASSERT
    try {
      // Wait for the virtual thread to terminate due to the expected failure
      assertDoesNotThrow(() -> virtualThread.join(2000));

      // Polling to ensure the async uncaught exception handler has finished executing
      long startTime = System.currentTimeMillis();
      while (caughtException.get() == null && (System.currentTimeMillis() - startTime) < 500) {
        Thread.onSpinWait();
      }

      var exception = caughtException.get();
      assertNotNull(
          exception,
          "The virtual thread terminated, but the uncaught exception handler did not capture the error in time!");
      assertInstanceOf(RuntimeException.class, exception);

      var exceptionMessage = exception.getMessage();
      assertThat(exceptionMessage, is(equalTo("Error while running health socket server")));
    } finally {
      assertDoesNotThrow(healthSocketServer::stop);
    }
  }

  @Test
  @DisplayName("HealthSocketServer handles InterruptedException during stop")
  @SneakyThrows
  @SuppressWarnings("unchecked")
  void startAndStopWithInterruptedException() {
    // ARRANGE
    var serverThread = (AtomicReference<Thread>) mock(AtomicReference.class);
    var virtualThread = mock(Thread.class);

    when(serverThread.getAndSet(isNull())).thenReturn(virtualThread);
    doThrow(new InterruptedException("Test InterruptedException")).when(virtualThread).join();

    HealthSocketServer healthSocketServer = new HealthSocketServer(null, null, serverThread, null);

    // ACT & ASSERT
    assertDoesNotThrow(healthSocketServer::stop);
  }

  @Test
  @DisplayName("HealthSocketServer responds with HEALTHY status when endpoint returns UP")
  @SneakyThrows
  void startAndReceiveHealthyResponse() {
    // ARRANGE
    var timestamp = System.currentTimeMillis();
    var socketPath = "test." + timestamp + ".sock";
    var serverThread = new AtomicReference<Thread>(null);

    var mockHealth = mock(IndicatedHealthDescriptor.class);
    when(mockHealth.getStatus()).thenReturn(Status.UP);

    var mockEndpoint = mock(HealthEndpoint.class);
    when(mockEndpoint.health()).thenReturn(mockHealth);

    var healthSocketServer = new HealthSocketServer(socketPath, mockEndpoint, serverThread, null);

    // ACT & ASSERT
    assertDoesNotThrow(healthSocketServer::start);

    // Wait until the socket file is created
    var path = java.nio.file.Path.of(socketPath);
    long startTime = System.currentTimeMillis();
    while (!Files.exists(path) && (System.currentTimeMillis() - startTime) < 2000) {
      Thread.onSpinWait();
    }
    assertTrue(Files.exists(path), "The socket file was not created in time!");

    waitForServerToAcceptConnections(socketPath);

    // Connect and read the response
    var socketAddress = UnixDomainSocketAddress.of(socketPath);
    var socketChannel = SocketChannel.open(socketAddress);
    var buffer = ByteBuffer.allocate(1);
    int bytesRead = socketChannel.read(buffer);
    socketChannel.close();

    // ASSERT - should receive 'H' for healthy
    assertThat(bytesRead, is(equalTo(1)));
    assertThat((char) buffer.get(0), is(equalTo('H')));

    assertDoesNotThrow(healthSocketServer::stop);
  }

  @Test
  @DisplayName("HealthSocketServer responds with UNHEALTHY status when endpoint returns non-UP")
  @SneakyThrows
  void startAndReceiveUnhealthyResponse() {
    // ARRANGE
    var timestamp = System.currentTimeMillis();
    var socketPath = "test2." + timestamp + ".sock";
    var serverThread = new AtomicReference<Thread>(null);

    var mockHealth = mock(IndicatedHealthDescriptor.class);
    when(mockHealth.getStatus()).thenReturn(Status.DOWN);

    var mockEndpoint = mock(HealthEndpoint.class);
    when(mockEndpoint.health()).thenReturn(mockHealth);

    var healthSocketServer = new HealthSocketServer(socketPath, mockEndpoint, serverThread, null);

    // ACT & ASSERT
    assertDoesNotThrow(healthSocketServer::start);

    // Wait until the socket file is created
    var path = java.nio.file.Path.of(socketPath);
    long startTime = System.currentTimeMillis();
    while (!Files.exists(path) && (System.currentTimeMillis() - startTime) < 2000) {
      Thread.onSpinWait();
    }
    assertTrue(Files.exists(path), "The socket file was not created in time!");

    waitForServerToAcceptConnections(socketPath);

    // Connect and read the response
    var socketAddress = UnixDomainSocketAddress.of(socketPath);
    var socketChannel = SocketChannel.open(socketAddress);
    var buffer = ByteBuffer.allocate(1);
    int bytesRead = socketChannel.read(buffer);
    socketChannel.close();

    // ASSERT - should receive 'U' for unhealthy
    assertThat(bytesRead, is(equalTo(1)));
    assertThat((char) buffer.get(0), is(equalTo('U')));

    assertDoesNotThrow(healthSocketServer::stop);
  }

  @Test
  @DisplayName("HealthSocketServer with null socketPath - cleanup should not throw")
  @SneakyThrows
  @SuppressWarnings("unchecked")
  void stopWithNullSocketPath() {
    // ARRANGE
    var serverThread = (AtomicReference<Thread>) mock(AtomicReference.class);
    var virtualThread = mock(Thread.class);

    when(serverThread.getAndSet(isNull())).thenReturn(virtualThread);

    HealthSocketServer healthSocketServer = new HealthSocketServer(null, null, serverThread, null);

    // ACT & ASSERT - should not throw when socketPath is null
    assertDoesNotThrow(healthSocketServer::stop);
  }

  @Test
  @DisplayName("HealthSocketServer stop removes socket file")
  @SneakyThrows
  void stopCleansUpSocketFile() {
    // ARRANGE
    var timestamp = System.currentTimeMillis();
    var socketPath = "test3." + timestamp + ".sock";
    var serverThread = new AtomicReference<Thread>(null);

    var healthSocketServer = new HealthSocketServer(socketPath, null, serverThread, null);

    // ACT
    assertDoesNotThrow(healthSocketServer::start);

    // Wait until the socket file is created
    var path = Path.of(socketPath);
    long startTime = System.currentTimeMillis();
    while (!Files.exists(path) && (System.currentTimeMillis() - startTime) < 1000) {
      Thread.onSpinWait();
    }
    assertTrue(Files.exists(path), "The socket file was not created in time!");

    // ACT - stop and verify cleanup
    assertDoesNotThrow(healthSocketServer::stop);

    // ASSERT - socket file should be deleted
    startTime = System.currentTimeMillis();
    while (Files.exists(path) && (System.currentTimeMillis() - startTime) < 1000) {
      Thread.onSpinWait();
    }
    assertFalse(Files.exists(path), "The socket file was not cleaned up!");
  }

  @Test
  @DisplayName("HealthSocketServer handles IOException during socket file cleanup")
  @SneakyThrows
  void cleanupSocketFileHandlesIOException() {
    // ARRANGE
    var tempDirectory = Files.createTempDirectory("health-socket-cleanup");
    var socketPath = tempDirectory.resolve("test-cleanup.sock");
    Files.createFile(socketPath);

    var originalPermissions = Files.getPosixFilePermissions(tempDirectory);
    Files.setPosixFilePermissions(tempDirectory, PosixFilePermissions.fromString("r-xr-xr-x"));

    var healthSocketServer =
        new HealthSocketServer(socketPath.toString(), null, new AtomicReference<>(), null);
    var cleanupMethod = HealthSocketServer.class.getDeclaredMethod("cleanupSocketFile");
    cleanupMethod.setAccessible(true);

    try {
      // ACT & ASSERT - the deletion failure should be swallowed and only logged
      assertDoesNotThrow(() -> cleanupMethod.invoke(healthSocketServer));
    } finally {
      Files.setPosixFilePermissions(tempDirectory, originalPermissions);
      Files.deleteIfExists(socketPath);
      Files.deleteIfExists(tempDirectory);
    }
  }

  @Test
  @DisplayName("HealthSocketServer calls uncaughtExceptionHandler when provided on initialization")
  @SneakyThrows
  void startWithCustomUncaughtExceptionHandler() {
    // ARRANGE
    var socketPath = "/invalid/path/uncaught/handler/test.sock";
    var serverThread = new AtomicReference<Thread>(null);
    var caughtException = new AtomicReference<Throwable>();

    var customHandler =
        (Thread.UncaughtExceptionHandler) (_, throwable) -> caughtException.set(throwable);

    var healthSocketServer = new HealthSocketServer(socketPath, null, serverThread, customHandler);

    // ACT & ASSERT
    assertDoesNotThrow(healthSocketServer::start);

    var virtualThread = serverThread.get();

    try {
      // Wait for the virtual thread to terminate due to the expected failure
      assertDoesNotThrow(() -> virtualThread.join(2000));

      // Polling to ensure the async uncaught exception handler has finished executing
      long startTime = System.currentTimeMillis();
      while (caughtException.get() == null && (System.currentTimeMillis() - startTime) < 500) {
        Thread.onSpinWait();
      }

      var exception = caughtException.get();
      assertNotNull(exception, "Custom uncaught exception handler should have been called!");
      assertInstanceOf(RuntimeException.class, exception);
    } finally {
      assertDoesNotThrow(healthSocketServer::stop);
    }
  }

  @Test
  @DisplayName("HealthSocketServer multiple clients can connect and receive responses")
  @SneakyThrows
  void multipleClientsCanConnectAndReceiveResponses() {
    // ARRANGE
    var timestamp = System.currentTimeMillis();
    var socketPath = "test4." + timestamp + ".sock";
    var serverThread = new AtomicReference<Thread>(null);

    var mockHealth = mock(IndicatedHealthDescriptor.class);
    when(mockHealth.getStatus()).thenReturn(Status.UP);

    var mockEndpoint = mock(HealthEndpoint.class);
    when(mockEndpoint.health()).thenReturn(mockHealth);

    var healthSocketServer = new HealthSocketServer(socketPath, mockEndpoint, serverThread, null);

    // ACT & ASSERT
    assertDoesNotThrow(healthSocketServer::start);

    // Wait until the socket file is created
    var path = java.nio.file.Path.of(socketPath);
    long startTime = System.currentTimeMillis();
    while (!Files.exists(path) && (System.currentTimeMillis() - startTime) < 2000) {
      Thread.onSpinWait();
    }
    assertTrue(Files.exists(path), "The socket file was not created in time!");

    waitForServerToAcceptConnections(socketPath);

    // Create multiple concurrent connections
    var socketAddress = UnixDomainSocketAddress.of(socketPath);
    for (int i = 0; i < 3; i++) {
      var socketChannel = SocketChannel.open(socketAddress);
      var buffer = ByteBuffer.allocate(1);
      int bytesRead = socketChannel.read(buffer);
      socketChannel.close();

      assertThat(bytesRead, is(equalTo(1)));
      assertThat((char) buffer.get(0), is(equalTo('H')));
    }

    assertDoesNotThrow(healthSocketServer::stop);
  }

  @Test
  @DisplayName("HealthSocketServer logs warning when client disconnects before response is sent")
  @SneakyThrows
  void clientDisconnectsBeforeResponseSent() {
    // ARRANGE
    var mockHealth = mock(IndicatedHealthDescriptor.class);
    when(mockHealth.getStatus()).thenReturn(Status.UP);

    var mockEndpoint = mock(HealthEndpoint.class);
    when(mockEndpoint.health()).thenReturn(mockHealth);

    var serverThread = new AtomicReference<Thread>(null);
    var healthSocketServer = new HealthSocketServer("test.sock", mockEndpoint, serverThread, null);

    // Mock SocketChannel that throws IOException on write
    var mockClient = mock(SocketChannel.class);
    doThrow(new IOException("Client disconnected")).when(mockClient).write(any(ByteBuffer.class));

    // ACT & ASSERT - invoke the private respond method using reflection
    Method respondMethod =
        HealthSocketServer.class.getDeclaredMethod("respond", SocketChannel.class);
    respondMethod.setAccessible(true);
    assertDoesNotThrow(() -> respondMethod.invoke(healthSocketServer, mockClient));
  }

  @SneakyThrows
  private void waitForServerToAcceptConnections(String socketPath) {
    var socketAddress = UnixDomainSocketAddress.of(socketPath);
    long startTime = System.currentTimeMillis();
    while ((System.currentTimeMillis() - startTime) < 2000) {
      try (var socketChannel = SocketChannel.open(socketAddress)) {
        socketChannel.close();
        return;
      } catch (IOException _) {
        Thread.onSpinWait();
      }
    }

    throw new AssertionError("The socket server was not ready to accept connections in time!");
  }
}
