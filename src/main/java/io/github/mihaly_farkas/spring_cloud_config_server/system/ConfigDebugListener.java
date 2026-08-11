package io.github.mihaly_farkas.spring_cloud_config_server.system;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

@Slf4j
public class ConfigDebugListener implements ApplicationListener<ApplicationPreparedEvent> {

  @Override
  @SuppressWarnings("java:S2068")
  public void onApplicationEvent(@NonNull ApplicationPreparedEvent event) {
    if (log.isDebugEnabled()) {
      ConfigurableEnvironment env = event.getApplicationContext().getEnvironment();

      log.debug(
          "spring.cloud.config.server.git.uri={}",
          env.getProperty("spring.cloud.config.server.git.uri"));
      log.debug(
          "spring.cloud.config.server.git.skipSslValidation={}",
          env.getProperty("spring.cloud.config.server.git.skipSslValidation"));
      log.debug(
          "spring.cloud.config.server.git.username={}",
          env.getProperty("spring.cloud.config.server.git.username"));
      log.debug(
          "spring.cloud.config.server.git.password={}",
          maskValue(env.getProperty("spring.cloud.config.server.git.password")));
      log.debug(
          "spring.cloud.config.server.git.refresh-rate={}",
          env.getProperty("spring.cloud.config.server.git.refresh-rate"));

      log.debug("encrypt.key={}", maskValue(env.getProperty("encrypt.key")));
    }
  }

  private String maskValue(String value) {
    String maskedValue;
    if (value != null && value.length() > 10) {
      int hiddenCharacters = value.length() - 5;
      maskedValue =
          value.substring(0, 5)
              + "*".repeat(hiddenCharacters)
              + value.substring(value.length() - 2);
    } else if (value != null && value.length() > 5) {
      int hiddenCharacters = Math.max(0, value.length() - 2);
      maskedValue =
          value.charAt(0) + "*".repeat(hiddenCharacters) + value.charAt(value.length() - 1);
    } else if (value != null) {
      maskedValue = "*".repeat(value.length());
    } else {
      maskedValue = null;
    }
    return maskedValue;
  }
}
