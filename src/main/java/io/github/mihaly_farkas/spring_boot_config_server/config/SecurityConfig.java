package io.github.mihaly_farkas.spring_boot_config_server.config;

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

/**
 * Spring Security configuration for the Spring Boot Config Server application.
 *
 * <p>This configuration class defines multiple security filter chains to handle different
 * authentication scenarios:
 *
 * <ul>
 *   <li><strong>Actuator Health Endpoint Chain:</strong> When Spring Security credentials are
 *       configured, this chain allows unauthenticated access to the health endpoint via {@code
 *       /actuator/health} (or custom actuator base path).
 *   <li><strong>Default Security Chain:</strong> When Spring Security credentials are configured,
 *       this chain requires HTTP Basic authentication for all other requests.
 *   <li><strong>Disabled Security Chain:</strong> When the {@code no_auth} profile is active, all
 *       requests are permitted without authentication.
 * </ul>
 *
 * <p>All security chains are configured as stateless (no session creation) since this is an
 * API-only application used by other applications, CLIs, scripts, and curl. CSRF protection is
 * enabled by default and can be disabled explicitly via the {@code
 * mihaly-farkas.spring-boot-config-server.security.disable-csrf} property when the deployment
 * environment does not require it.
 *
 * @author Mihály Farkas
 * @see org.springframework.security.web.SecurityFilterChain
 * @see org.springframework.security.config.annotation.web.builders.HttpSecurity
 */
@Configuration
@Slf4j
public class SecurityConfig {

  /**
   * Creates a dedicated security filter chain for the actuator health endpoint.
   *
   * <p>This chain matches only the {@code /health} endpoint under the configured actuator base path
   * and allows requests without authentication.
   *
   * @param http the {@link HttpSecurity} builder used to configure web security
   * @param actuatorBasePath the configured actuator base path (defaults to {@code /actuator})
   * @return the configured {@link SecurityFilterChain} that permits health endpoint requests
   */
  @Bean
  @ConditionalOnProperty(name = "spring.security.user.password")
  @Profile("!no_auth")
  @Order(100)
  @SuppressWarnings("java:S4502")
  public SecurityFilterChain actuatorHealthSecurityFilterChain(
      HttpSecurity http,
      @Value("${management.endpoints.web.base-path:/actuator}") String actuatorBasePath) {
    log.info(
        "Configuring SecurityFilterChain with order 100 to allow '{}/health' requests",
        actuatorBasePath);
    return http.securityMatcher(actuatorBasePath + "/health")
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .sessionManagement(sess -> sess.sessionCreationPolicy(STATELESS))
        .build();
  }

  /**
   * Creates the default security filter chain when Spring Security credentials are configured.
   *
   * <p>This chain requires authentication for every request using HTTP Basic auth.
   *
   * @param http the {@link HttpSecurity} builder used to configure web security
   * @return the configured {@link SecurityFilterChain} that authenticates all requests
   */
  @Bean
  @ConditionalOnProperty(name = "spring.security.user.password")
  @Profile("!no_auth")
  @SuppressWarnings("java:S4502")
  public SecurityFilterChain defaultSecurityFilterChain(
      HttpSecurity http,
      @Value("${mihaly-farkas.spring-boot-config-server.security.disable-csrf:false}")
          boolean disableCsrf) {
    log.info(
        "Configuring SecurityFilterChain"
            + " with default order"
            + " to allow any request"
            + " with the configured Spring Security credentials");
    return http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .httpBasic(Customizer.withDefaults())
        .csrf(disableCsrf ? CsrfConfigurer::disable : Customizer.withDefaults())
        .sessionManagement(sess -> sess.sessionCreationPolicy(STATELESS))
        .build();
  }

  /**
   * Creates a security filter chain for the {@code no_auth} profile.
   *
   * <p>When this profile is active, every incoming request is permitted without authentication.
   *
   * @param http the {@link HttpSecurity} builder used to configure web security
   * @param disableCsrf whether CSRF protection should be disabled (defaults to {@code false})
   * @return the configured {@link SecurityFilterChain} that allows all requests
   */
  @Bean
  @Profile("no_auth")
  @SuppressWarnings("java:S4502")
  public SecurityFilterChain disabledSpringBootSecuritySecurityFilterChain(
      HttpSecurity http,
      @Value("${mihaly-farkas.spring-boot-config-server.security.disable-csrf:false}")
          boolean disableCsrf) {
    log.info(
        "Configuring SecurityFilterChain"
            + " with default order"
            + " to allow ALL requests"
            + " without authentication");
    return http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .csrf(disableCsrf ? CsrfConfigurer::disable : Customizer.withDefaults())
        .sessionManagement(sess -> sess.sessionCreationPolicy(STATELESS))
        .build();
  }
}
