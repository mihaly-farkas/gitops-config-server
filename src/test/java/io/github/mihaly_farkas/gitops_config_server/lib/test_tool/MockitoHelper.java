package io.github.mihaly_farkas.gitops_config_server.lib.test_tool;

public final class MockitoHelper {

  private MockitoHelper() {
    // private constructor to prevent instantiation
  }

  public static void assertVerify(String message, Runnable verifyBlock) {
    try {
      verifyBlock.run();
    } catch (AssertionError e) {
      // Új hibát dobunk a saját üzenetünkkel, megőrizve az eredeti Mockito nyomkövetést
      throw new AssertionError(message + "\nDetails: " + e.getMessage(), e);
    }
  }
}
