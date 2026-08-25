package io.github.mihaly_farkas.gitops_config_server.lib.test_tool;

public final class MockitoHelper {

  private MockitoHelper() {
    // private constructor to prevent instantiation
  }

  public static void assertVerify(String message, Runnable verifyBlock) {
    try {
      verifyBlock.run();
    } catch (AssertionError e) {
      // Re-throw with our own message while preserving the original Mockito stack trace.
      throw new AssertionError(message + "\nDetails: " + e.getMessage(), e);
    }
  }
}
