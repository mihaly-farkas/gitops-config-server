package io.github.mihaly_farkas.spring_boot_config_server.lib.test_tool;

import static ch.qos.logback.classic.Level.TRACE;
import static java.lang.Thread.sleep;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.LockSupport;
import org.apache.logging.log4j.LogManager;
import org.slf4j.LoggerFactory;

public class TestLogAppender extends AppenderBase<ILoggingEvent> {
  final List<ILoggingEvent> loggingEvents = new CopyOnWriteArrayList<>();
  private final Logger logger;

  private TestLogAppender(Logger logger) {
    this.logger = logger;
  }

  public static TestLogAppender attachTo(Class<?> loggerClass) {
    var logger = (Logger) LoggerFactory.getLogger(loggerClass);
    logger.setLevel(TRACE);
    var appender = new TestLogAppender(logger);
    appender.start();
    logger.addAppender(appender);
    return appender;
  }

  public void detach() {
    int lastSize;
    int currentSize = loggingEvents.size();

    // Max wait time of 2 seconds to ensure that the background logging has completed
    long deadline = System.currentTimeMillis() + 2000;

    do {
      lastSize = currentSize;
      LockSupport.parkNanos(10_000_000L);
      currentSize = loggingEvents.size();
    } while (currentSize > lastSize && System.currentTimeMillis() < deadline);

    if (logger != null) {
      logger.detachAppender(this);
    }
    stop();
  }

  @Override
  protected void append(ILoggingEvent eventObject) {
    loggingEvents.add(eventObject);
  }

  @Override
  public String toString() {
    var description = new StringBuilder("log records in order:\n");

    for (var loggingEvent : loggingEvents) {
      description
          .repeat(" ", 17)
          .append(String.format("%-7s", "<" + loggingEvent.getLevel() + ">"))
          .append(" \"")
          .append(loggingEvent.getFormattedMessage())
          .append("\"\n");

      var throwableProxy = loggingEvent.getThrowableProxy();
      if (throwableProxy != null) {
        var stackTrace = ch.qos.logback.classic.spi.ThrowableProxyUtil.asString(throwableProxy);
        for (String line : stackTrace.split("\n")) {
          description.repeat(" ", 17).append("   ").append(line).append("\n");
        }
      }
    }

    return description.toString();
  }

  @SuppressWarnings({"ResultOfMethodCallIgnored", "java:S2925"})
  public void flushLogs() {
    LogManager.getFactory();
    try {
      sleep(10);
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt();
    }
  }
}
