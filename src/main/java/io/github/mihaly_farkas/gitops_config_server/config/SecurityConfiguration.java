package io.github.mihaly_farkas.gitops_config_server.config;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Slf4j
public class SecurityConfiguration {

  @Bean
  @ConditionalOnProperty(name = "spring.security.user.password")
  @Order(100)
  @SuppressWarnings("java:S4502")
  public SecurityFilterChain actuatorHealthSecurityFilterChain(
      HttpSecurity http,
      @Value("${management.endpoints.web.base-path:/actuator}") String actuatorBasePath) {
    log.info(
        "Configuring SecurityFilterChain" + " with order 100" + " to allow '{}/health' requests",
        actuatorBasePath);
    return http.securityMatcher(actuatorBasePath + "/health")
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        // This is a stateless, API-only application used by other applications, CLIs, scripts,
        // curl, etc. The application is not used by browsers and does not use cookies or sessions.
        // Authentication is handled through the Authorization header, so CSRF protection is not
        // applicable.
        .csrf(CsrfConfigurer::disable)
        .sessionManagement(sess -> sess.sessionCreationPolicy(STATELESS))
        .build();
  }

  @Bean
  @ConditionalOnProperty(name = "spring.security.user.password")
  @SuppressWarnings("java:S4502")
  public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) {
    log.info(
        "Configuring SecurityFilterChain"
            + " with default order"
            + " to allow any request"
            + " with the configured Spring Security credentials");
    return http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .httpBasic(Customizer.withDefaults())
        // This is a stateless, API-only application used by other applications, CLIs, scripts,
        // curl, etc. The application is not used by browsers and does not use cookies or sessions.
        // Authentication is handled through the Authorization header, so CSRF protection is not
        // applicable.
        .csrf(CsrfConfigurer::disable)
        .sessionManagement(sess -> sess.sessionCreationPolicy(STATELESS))
        .build();
  }

  @Bean
  @Profile("disable_spring_security")
  @SuppressWarnings("java:S4502")
  public SecurityFilterChain disabledSpringBootSecuritySecurityFilterChain(HttpSecurity http) {
    log.info(
        "Configuring SecurityFilterChain"
            + " with default order"
            + " to allow ALL requests"
            + " without authentication");
    return http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        // This is a stateless, API-only application used by other applications, CLIs, scripts,
        // curl, etc. The application is not used by browsers and does not use cookies or sessions.
        // Authentication is handled through the Authorization header, so CSRF protection is not
        // applicable.
        .csrf(CsrfConfigurer::disable)
        .sessionManagement(sess -> sess.sessionCreationPolicy(STATELESS))
        .build();
  }
}
