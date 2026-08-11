package io.github.mihaly_farkas.gitops_config_server;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({
  "mockmvc_public_github_repo",
  "mockmvc_all_actuator_endpoints",
  "mockmvc_spring_security"
})
class SpringSecurityConfiguredTest extends SpringSecurityTest {

  @Value("${spring.security.user.name}")
  String springSecurityUsername;

  @Value("${spring.security.user.password}")
  String springSecurityPassword;

  @ParameterizedTest(name = "endpoint -> {0}")
  @MethodSource("openEndpoints")
  @DisplayName(
      "When Spring Security is configured,"
          + " open endpoints are accessible without authorization")
  @SneakyThrows
  void openEndpointsAreAccessibleWithoutAuth(String endpoint, String expectedContentType) {
    // ACT & ASSERT
    mockMvc
        .perform(get(endpoint))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(expectedContentType));
  }

  @ParameterizedTest(name = "endpoint -> {0}")
  @MethodSource("protectedEndpoints")
  @DisplayName(
      "When Spring Security is configured,"
          + " protected endpoints are accessible with authorization")
  @SneakyThrows
  void protectedEndpointAreAccessibleWithAuth(String endpoint, String expectedContentType) {
    // ACT & ASSERT
    var result =
        mockMvc
            .perform(get(endpoint).with(httpBasic(springSecurityUsername, springSecurityPassword)))
            .andExpect(status().is(not(UNAUTHORIZED.value())));

    // ASSERT
    if (expectedContentType != null) {
      result.andExpect(content().contentTypeCompatibleWith(expectedContentType));
    } else {
      result.andExpect(header().doesNotExist(CONTENT_TYPE));
    }
  }

  @ParameterizedTest(name = "endpoint -> {0}")
  @MethodSource("protectedEndpoints")
  @DisplayName(
      "When Spring Security is configured,"
          + " protected endpoints are NOT accessible without authorization")
  @SneakyThrows
  void protectedEndpointAreNotAccessibleWithoutAuth(String endpoint) {
    // ACT & ASSERT
    mockMvc
        .perform(get(endpoint))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", matchesPattern("^Basic.*")));
  }

  @ParameterizedTest(name = "endpoint -> {0}")
  @MethodSource("protectedEndpoints")
  @DisplayName(
      "When Spring Security is configured,"
          + " protected endpoints are NOT accessible with invalid authorization")
  @SneakyThrows
  void protectedEndpointAreNotAccessibleWithWrongCredentials(String endpoint) {
    // ACT & ASSERT
    mockMvc
        .perform(get(endpoint).with(httpBasic("admin", "wrong-password")))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", matchesPattern("^Basic.*")));
  }
}
