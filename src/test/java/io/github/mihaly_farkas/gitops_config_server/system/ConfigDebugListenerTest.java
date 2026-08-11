package io.github.mihaly_farkas.gitops_config_server.system;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

@ExtendWith(MockitoExtension.class)
class ConfigDebugListenerTest {

  @Mock private ApplicationPreparedEvent event;

  @Mock private ConfigurableEnvironment environment;

  @Mock private ConfigurableApplicationContext applicationContext;

  private Logger logger;
  private ListAppender<ILoggingEvent> logAppender;
  private Level originalLevel;

  private static Stream<Arguments> debugLoggingCases() {
    return Stream.of(
        Arguments.of(
            "spring.cloud.config.server.git.uri",
            "https://config.example",
            "https://config.example"),
        Arguments.of("spring.cloud.config.server.git.username", "svc-user", "svc-user"),
        Arguments.of(
            "spring.cloud.config.server.git.password", "VerySecretPwd1", "VeryS*********d1"),
        Arguments.of("spring.cloud.config.server.git.refresh-rate", "30000", "30000"),
        Arguments.of("encrypt.key", "abcd", "****"),
        Arguments.of("encrypt.key", "abcdef", "a****f"),
        Arguments.of("encrypt.key", null, "null"));
  }

  @BeforeEach
  void setUp() {
    logger = (Logger) LoggerFactory.getLogger(ConfigDebugListener.class);
    originalLevel = logger.getLevel();

    logAppender = new ListAppender<>();
    logAppender.start();
    logger.addAppender(logAppender);
    logger.setLevel(Level.INFO);
  }

  @AfterEach
  void tearDown() {
    logger.detachAppender(logAppender);
    logger.setLevel(originalLevel);
  }

  @Test
  @DisplayName("ConfigDebugListener does not log anything when debug is disabled")
  void configDebugListenerDoesNotLogWhenDebugIsDisabled() {
    // ACT
    new ConfigDebugListener().onApplicationEvent(event);

    // ASSERT
    assertAll(
        () -> assertEquals(0, logAppender.list.size()), () -> verifyNoInteractions(environment));
  }

  @ParameterizedTest(name = "{index}: {0}={2}")
  @DisplayName("Logs the configured key and value when debug is enabled")
  @MethodSource("debugLoggingCases")
  void logsExpectedLineWhenDebugIsEnabled(
      String key, String propertyValue, String expectedLogValue) {
    // ARRANGE
    logger.setLevel(Level.DEBUG);

    when(event.getApplicationContext()).thenReturn(applicationContext);
    when(applicationContext.getEnvironment()).thenReturn(environment);
    when(environment.getProperty(anyString()))
        .thenAnswer(
            invocation -> {
              var requestedKey = invocation.getArgument(0, String.class);
              return key.equals(requestedKey) ? propertyValue : null;
            });

    // ACT
    new ConfigDebugListener().onApplicationEvent(event);

    // ASSERT
    assertTrue(
        logAppender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .anyMatch((key + "=" + expectedLogValue)::equals));
  }
}
