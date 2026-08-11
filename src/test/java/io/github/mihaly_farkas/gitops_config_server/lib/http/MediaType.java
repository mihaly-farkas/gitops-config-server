package io.github.mihaly_farkas.gitops_config_server.lib.http;

public final class MediaType {

  public static final String APPLICATION_SPRING_BOOT_ACTUATOR_JSON_VALUE =
      "application/vnd.spring-boot.actuator.v3+json";

  public static final org.springframework.http.MediaType APPLICATION_SPRING_BOOT_ACTUATOR_JSON =
      org.springframework.http.MediaType.valueOf(APPLICATION_SPRING_BOOT_ACTUATOR_JSON_VALUE);

  private MediaType() {
    // Prevent instantiation
  }
}
