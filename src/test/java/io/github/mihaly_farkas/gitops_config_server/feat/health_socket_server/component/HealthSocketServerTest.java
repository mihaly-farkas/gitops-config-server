package io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

import io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component.default_command.DefaultRespondCommand;
import io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component.default_command.DefaultRunCommand;
import io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server.component.default_command.DefaultStopCommand;
import jakarta.validation.constraints.NotNull;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicReference;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;

class HealthSocketServerTest {

  private static @NotNull TestBuilder testBuilder() {
    return new TestBuilder();
  }

  @DisplayName("HealthSocketServer stops gracefully when the virtual thread is interrupted")
  @Test
  void whenInterrupted() {
    // ARRANGE
    var testBuilder = testBuilder();

    try (var healthSocketServer = testBuilder.build()) {
      healthSocketServer.start();
      var virtualThread = testBuilder.serverThread.get();

      // ACT
      interruptAndJoin(virtualThread);

      // ASSERT
      assertThat(testBuilder.serverThread.get(), is(nullValue()));
    }
  }

  @DisplayName("HealthSocketServer stops gracefully when the run command throws an exception")
  @Test
  @SneakyThrows
  void whenRunCommandThrowsAnException() {
    var testBuilder =
        testBuilder()
            .runCommand(
                (_, _, _) -> {
                  throw new RuntimeException("Test Exception");
                });

    try (var healthSocketServer = testBuilder.build()) {
      healthSocketServer.start();
      var serverThread = testBuilder.serverThread;
      var virtualThread = testBuilder.serverThread.get();

      // ACT
      readSocketAndJoin(testBuilder, virtualThread);

      // ASSERT
      assertThat(serverThread.get(), is(nullValue()));
    }
  }

  private void readSocketAndJoin(TestBuilder testBuilder, Thread virtualThread)
      throws InterruptedException {
    readSocket(testBuilder);
    virtualThread.join(1000);
  }

  private void readSocket(TestBuilder testBuilder) {
    var socketFilename = testBuilder.socketFilename;
    var socketAddress = UnixDomainSocketAddress.of(socketFilename);
    try (var socketChannel = SocketChannel.open(socketAddress)) {
      var buffer = ByteBuffer.allocate(1);
      socketChannel.read(buffer);
    } catch (Exception _) {
      // Ignore the exception, as we expect it to be thrown due to the mocked health endpoint
    }
  }

  @SneakyThrows
  private void interruptAndJoin(Thread virtualThread) {
    virtualThread.interrupt();
    virtualThread.join(1000);
  }

  private static class TestBuilder {
    private String tmpDir;
    private String uuid;
    private String testId;
    private String socketFilename;
    private Thread.Builder.OfVirtual virtualThreadBuilder;
    private AtomicReference<Thread> serverThread;
    private HealthEndpoint healthEndpoint;
    private RespondCommand respondCommand;
    private RunCommand runCommand;
    private StopCommand stopCommand;

    public TestBuilder runCommand(RunCommand runCommand) {
      this.runCommand = runCommand;
      return this;
    }

    public HealthSocketServer build() {
      if (tmpDir == null) tmpDir = System.getProperty("java.io.tmpdir");
      if (uuid == null) uuid = randomUUID().toString();
      if (testId == null) testId = "health-socket-server-test-" + uuid;
      if (socketFilename == null) socketFilename = tmpDir + "/" + testId + ".sock";
      if (virtualThreadBuilder == null) virtualThreadBuilder = Thread.ofVirtual().name(testId);
      if (serverThread == null) serverThread = new AtomicReference<>();
      if (runCommand == null) {
        if (respondCommand == null) {
          if (healthEndpoint == null) {
            healthEndpoint = mock(HealthEndpoint.class);
          }
          respondCommand = new DefaultRespondCommand(healthEndpoint);
        }
        runCommand = new DefaultRunCommand(respondCommand);
      }
      if (stopCommand == null) stopCommand = new DefaultStopCommand();

      return new HealthSocketServer(
          socketFilename, virtualThreadBuilder, serverThread, runCommand, stopCommand);
    }
  }
}
