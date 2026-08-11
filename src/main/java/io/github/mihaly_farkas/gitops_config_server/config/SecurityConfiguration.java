package io.github.mihaly_farkas.gitops_config_server.config;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {

  @Bean
  @SuppressWarnings("java:S4502")
  public SecurityFilterChain securityFilterChain(HttpSecurity http) {
    return http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .httpBasic(Customizer.withDefaults())
        .csrf(CsrfConfigurer::disable)
        .sessionManagement(sess -> sess.sessionCreationPolicy(STATELESS))
        .build();
  }
}
