package io.github.mihaly_farkas.spring_boot_config_server.lib.test_tool;

import ch.qos.logback.classic.Level;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matcher;

@Slf4j
public class TestLogAppenderMatcher {

  public static LogEntry logEntry(Level expectedLogLevel, String expectedLogMessage) {
    return new LogEntry(expectedLogLevel, expectedLogMessage, ComparisonMode.EXACT);
  }

  public static LogEntry logEntry(
      Level expectedLogLevel, String expectedLogMessage, ComparisonMode comparisonMode) {
    return new LogEntry(expectedLogLevel, expectedLogMessage, comparisonMode);
  }

  public static Matcher<TestLogAppender> logged(Level expectedLogLevel, String expectedLogMessage) {
    return logged(logEntry(expectedLogLevel, expectedLogMessage));
  }

  public static Matcher<TestLogAppender> logged(LogEntry logEntry) {

    return new org.hamcrest.TypeSafeMatcher<>() {
      @Override
      protected boolean matchesSafely(TestLogAppender item) {
        return switch (logEntry.comparisonMode()) {
          case EXACT ->
              item.loggingEvents.stream()
                  .anyMatch(
                      event ->
                          logEntry.expectedLogLevel().equals(event.getLevel())
                              && logEntry.expectedLogMessage().equals(event.getFormattedMessage()));
          case CONTAINS ->
              item.loggingEvents.stream()
                  .anyMatch(
                      event ->
                          logEntry.expectedLogLevel().equals(event.getLevel())
                              && event
                                  .getFormattedMessage()
                                  .contains(logEntry.expectedLogMessage()));
        };
      }

      @Override
      public void describeTo(org.hamcrest.Description description) {
        description
            .appendText("a log record with expectedLogLevel ")
            .appendValue(logEntry.expectedLogLevel())
            .appendText(" and expectedLogMessage ")
            .appendValue(logEntry.expectedLogMessage());
      }
    };
  }

  public static Matcher<TestLogAppender> loggedInOrder(LogEntry... logEntries) {
    return new org.hamcrest.TypeSafeMatcher<>() {
      @Override
      protected boolean matchesSafely(TestLogAppender item) {
        int lastIndex = -1;
        for (var logEntry : logEntries) {
          boolean found = false;
          for (int i = lastIndex + 1; i < item.loggingEvents.size(); i++) {
            boolean matches = isMatches(item, logEntry, i);
            if (matches) {
              lastIndex = i;
              found = true;
              break;
            }
          }
          if (!found) {
            log.warn(
                "Expected log entry not found: level={}, message={}",
                logEntry.expectedLogLevel(),
                logEntry.expectedLogMessage());
            return false;
          }
        }
        return true;
      }

      @Override
      public void describeTo(org.hamcrest.Description hamcrestDescription) {
        var description = new StringBuilder("<log records in order:\n");

        for (var logEntry : logEntries) {
          description
              .repeat(" ", 17)
              .append(String.format("%-7s", "<" + logEntry.expectedLogLevel() + ">"))
              .append(" \"")
              .append(logEntry.expectedLogMessage())
              .append("\"\n");
        }

        description.append(">");
        hamcrestDescription.appendText(description.toString());
      }
    };
  }

  private static boolean isMatches(TestLogAppender item, LogEntry logEntry, int i) {
    var event = item.loggingEvents.get(i);
    return switch (logEntry.comparisonMode()) {
      case EXACT ->
          logEntry.expectedLogLevel().equals(event.getLevel())
              && logEntry.expectedLogMessage().equals(event.getFormattedMessage());
      case CONTAINS ->
          logEntry.expectedLogLevel().equals(event.getLevel())
              && event.getFormattedMessage().contains(logEntry.expectedLogMessage());
    };
  }

  public enum ComparisonMode {
    EXACT,
    CONTAINS
  }

  public record LogEntry(
      Level expectedLogLevel, String expectedLogMessage, ComparisonMode comparisonMode) {}
}
