package io.github.mihaly_farkas.spring_boot_config_server.lib.test_tool;

import lombok.SneakyThrows;

public final class MockitoHelper {

  private MockitoHelper() {
    // private constructor to prevent instantiation
  }

  @SneakyThrows
  public static void assertVerify(String message, ExceptionalRunnable verifyBlock) {
    try {
      verifyBlock.run();
    } catch (AssertionError e) {
      // Re-throw with our own message while preserving the original Mockito stack trace.
      throw new AssertionError(message + "\nDetails: " + e.getMessage(), e);
    }
  }

  @FunctionalInterface
  public interface ExceptionalRunnable {
    void run() throws Exception;
  }
}
