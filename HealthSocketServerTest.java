package io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("SameParameterValue")
class HealthSocketServerTest {

  private Path socketPath;
  private HealthSocketServer.VirtualThreadBuilderWrapper virtualThreadBuilderWrapper;
  private AtomicReference<Thread> workerThreadReference;

  private Thread workerThread;
  private ServerSocketChannel serverSocketChannel;
  private HealthSocketServer.DeleteIfExistsConsumer deleteIfExistsConsumer;

  private HealthSocketServer healthSocketServer;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() throws IOException {
    socketPath = mock(Path.class);
    virtualThreadBuilderWrapper = mock(HealthSocketServer.VirtualThreadBuilderWrapper.class);
    workerThreadReference = (AtomicReference<Thread>) mock(AtomicReference.class);

    workerThread = mock(Thread.class);
    serverSocketChannel = mock(ServerSocketChannel.class);
    deleteIfExistsConsumer = mock(HealthSocketServer.DeleteIfExistsConsumer.class);

    var socketPathname = "test-socket.sock";
    var serverSocketChannelSupplier = mock(HealthSocketServer.ServerSocketChannelSupplier.class);
    var unixDomainSocketAddressGetter =
      (Function<String, UnixDomainSocketAddress>) mock(Function.class);
    var unixDomainSocketAddress = mock(UnixDomainSocketAddress.class);

    healthSocketServer =
      new HealthSocketServer(
        socketPath,
        null,
        virtualThreadBuilderWrapper,
        workerThreadReference,
        serverSocketChannelSupplier,
        unixDomainSocketAddressGetter,
        deleteIfExistsConsumer);

    when(socketPath.toString()).thenReturn(socketPathname);
    when(serverSocketChannelSupplier.get()).thenReturn(serverSocketChannel);
    when(unixDomainSocketAddressGetter.apply(any())).thenReturn(unixDomainSocketAddress);
    when(unixDomainSocketAddressGetter.apply(socketPathname)).thenReturn(unixDomainSocketAddress);
  }

  @DisplayName("HealthSocketServer.start() handles gracefully if server is already created")
  @Test
  void startWhenAlreadyStarted() {
    // ARRANGE
    when(workerThreadReference.get()).thenReturn(workerThread);

    // ACT
    healthSocketServer.start();

    // ASSERT
    verify(virtualThreadBuilderWrapper, times(0).description("Worker thread should not be started"))
      .start(any());
    verify(workerThreadReference, times(0).description("Worker thread reference should not be set"))
      .set(any());
  }

  @DisplayName("HealthSocketServer.run() stops gracefully when the server thread is interrupted")
  @Test
  @SneakyThrows
  void runWhenThreadInterrupted() {
    // ARRANGE
    when(workerThread.isInterrupted()).thenReturn(true);
    when(workerThreadReference.getAndSet(null)).thenReturn(workerThread);

    // ACT
    healthSocketServer.run(workerThread);

    // ASSERT
    verify(
      serverSocketChannel,
      times(0).description("Socket channel should not accept connections"))
      .accept();
    assertWorkerThreadIsInterrupted("Virtual thread should be interrupted");
    assertWorkerTreadIsJoined("Worker threads should be waited on with a timeout for termination");
    assertSocketFileDeletionIsAttempted("Socket file should be deleted");
  }

  @DisplayName(
    "HealthSocketServer.close() stops gracefully when the server thread is interrupted during close process")
  @Test
  @SneakyThrows
  void closeWhenThreadInterruptedDuringClose() {
    // ARRANGE
    when(workerThreadReference.get()).thenReturn(workerThread);
    when(workerThreadReference.getAndSet(null)).thenReturn(workerThread);
    doThrow(new InterruptedException("Simulated interruption")).when(workerThread).join(anyLong());

    // ACT
    healthSocketServer.close();

    // ASSERT
    assertWorkerThreadIsInterrupted("Virtual thread should be interrupted two times", 2);
    assertWorkerTreadIsJoined("Worker threads should be waited on with a timeout for termination");
    assertSocketFileDeletionIsAttempted("Socket file should be deleted");
  }

  @DisplayName("HealthSocketServer.run() stops gracefully when the socket file deletion fails")
  @Test
  @SneakyThrows
  void closeWhenSocketFileDeletionFails() {
    // ARRANGE
    when(workerThreadReference.get()).thenReturn(workerThread);
    when(workerThreadReference.getAndSet(null)).thenReturn(workerThread);
    doThrow(new IOException("Simulated deletion failure"))
      .when(deleteIfExistsConsumer)
      .accept(any());

    // ACT
    healthSocketServer.close();

    // ASSERT
    assertWorkerThreadIsInterrupted("Virtual thread should be interrupted");
    assertWorkerTreadIsJoined("Worker threads should be waited on with a timeout for termination");
    assertSocketFileDeletionIsAttempted("Socket file deletion should be attempted");
  }

  private void assertWorkerThreadIsInterrupted(String description) {
    assertWorkerThreadIsInterrupted(description, 1);
  }

  private void assertWorkerThreadIsInterrupted(String description, int times) {
    verify(workerThread, times(times).description(description)).interrupt();
  }

  private void assertWorkerTreadIsJoined(String description) throws InterruptedException {
    verify(workerThread, times(1).description(description)).join(anyLong());
  }

  private void assertSocketFileDeletionIsAttempted(String description) throws IOException {
    verify(deleteIfExistsConsumer, times(1).description(description)).accept(socketPath);
  }

  //  @DisplayName(
  //      "HealthSocketServer.run() keeps serving probes after a client disconnect causes a broken
  // pipe")
  //  @Test
  //  @SneakyThrows
  //  @SuppressWarnings({"unchecked", "resource"})
  //  void runWhenProbeWriteFails() {
  //    // ARRANGE
  //    var socketPath = mock(Path.class);
  //    var virtualThreadReference = (AtomicReference<Thread>) mock(AtomicReference.class);
  //    var currentThreadSupplier = (Supplier<Thread>) mock(Supplier.class);
  //    var serverSocketChannelSupplier =
  // mock(HealthSocketServer.ServerSocketChannelSupplier.class);
  //    var unixDomainSocketAddressGetter =
  //        (Function<String, UnixDomainSocketAddress>) mock(Function.class);
  //    var deleteIfExistsConsumer = mock(HealthSocketServer.DeleteIfExistsConsumer.class);
  //    var healthEndpoint = mock(HealthEndpoint.class);
  //    var healthDescriptor = mock(IndicatedHealthDescriptor.class);
  //
  //    var socketPathname = "test-socket.sock";
  //    var virtualThread = mock(Thread.class);
  //    var serverSocketChannel = mock(ServerSocketChannel.class);
  //    var unixDomainSocketAddress = mock(UnixDomainSocketAddress.class);
  //    var firstClient = mock(SocketChannel.class);
  //    var secondClient = mock(SocketChannel.class);
  //
  //    when(currentThreadSupplier.get()).thenReturn(virtualThread);
  //    when(serverSocketChannelSupplier.get()).thenReturn(serverSocketChannel);
  //    when(unixDomainSocketAddressGetter.apply(any())).thenReturn(unixDomainSocketAddress);
  //    when(socketPath.toString()).thenReturn(socketPathname);
  //
  // when(unixDomainSocketAddressGetter.apply(socketPathname)).thenReturn(unixDomainSocketAddress);
  //    when(virtualThread.isInterrupted()).thenReturn(false, false, false, true);
  //    when(serverSocketChannel.accept()).thenReturn(firstClient, secondClient);
  //    when(virtualThreadReference.getAndSet(null)).thenReturn(virtualThread);
  //    when(healthEndpoint.health()).thenReturn(healthDescriptor);
  //    when(healthDescriptor.getStatus()).thenReturn(UP);
  //    doThrow(new IOException("Broken pipe")).when(firstClient).write(any(ByteBuffer.class));
  //    when(secondClient.write(any(ByteBuffer.class))).thenReturn(1);
  //
  //    var healthSocketServer =
  //        new HealthSocketServer(
  //            socketPath,
  //            healthEndpoint,
  //            null,
  //            virtualThreadReference,
  //            currentThreadSupplier,
  //            serverSocketChannelSupplier,
  //            unixDomainSocketAddressGetter,
  //            deleteIfExistsConsumer);
  //
  //    // ACT
  //    healthSocketServer.run();
  //
  //    // ASSERT
  //    verify(serverSocketChannel, times(2).description("Server should keep accepting probes"))
  //        .accept();
  //    verify(firstClient, times(1).description("First probe write should be attempted"))
  //        .write(any(ByteBuffer.class));
  //    verify(secondClient, times(1).description("Second probe should still be served"))
  //        .write(any(ByteBuffer.class));
  //    verify(deleteIfExistsConsumer, times(1).description("Socket file should be cleaned up"))
  //        .accept(socketPath);
  //  }
}
