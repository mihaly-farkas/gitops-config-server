package io.github.mihaly_farkas.spring_cloud_config_server;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({
  "public_github_repo",
  "all_actuator_endpoints",
  "encrypt_key",
  "spring_security_user"
})
class SpringSecurityConfiguredTest extends SpringSecurityTest {

  @DisplayName(
      "Unprotected endpoints are accessible without authorization when Spring Security user is configured")
  @MethodSource("unprotectedEndpoints")
  @ParameterizedTest(name = "endpoint -> {0}")
  @SneakyThrows
  void openEndpointsAreAccessibleWithoutAuth(
      String endpoint, HttpStatus expectedHttpStatus, MediaType expectedContentType) {
    // ARRANGE
    var expectedStatus = expectedHttpStatus.value();

    // ACT & ASSERT
    mockMvc
        .perform(get(endpoint))
        .andExpect(status().is(expectedStatus))
        .andExpect(content().contentTypeCompatibleWith(expectedContentType));
  }

  @DisplayName(
      "Protected endpoints are accessible with configured credentials when Spring Security user is configured")
  @MethodSource("protectedEndpoints")
  @ParameterizedTest(name = "endpoint -> {0}")
  @SneakyThrows
  void protectedEndpointsAreAccessibleWithAuth(
      String endpoint, HttpStatus expectedHttpStatus, MediaType expectedContentType) {
    // ARRANGE
    var expectedStatus = expectedHttpStatus.value();

    // ACT & ASSERT
    mockMvc
        .perform(get(endpoint).with(httpBasic(springSecurityUsername(), springSecurityPassword())))
        .andExpect(status().is(expectedStatus))
        .andExpect(
            expectedContentType != null
                ? content().contentTypeCompatibleWith(expectedContentType)
                : header().doesNotExist(CONTENT_TYPE));
  }

  @DisplayName(
      "Protected endpoints are NOT accessible without authorization when Spring Security user is configured")
  @MethodSource("protectedEndpoints")
  @ParameterizedTest(name = "endpoint -> {0}")
  @SneakyThrows
  void protectedEndpointsAreNotAccessibleWithoutAuth(String endpoint) {
    // ACT & ASSERT
    mockMvc
        .perform(get(endpoint))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", matchesPattern("^Basic.*")));
  }

  @DisplayName(
      "Protected endpoints are NOT accessible with invalid authorization when Spring Security user is configured")
  @MethodSource("protectedEndpoints")
  @ParameterizedTest(name = "endpoint -> {0}")
  @SneakyThrows
  void protectedEndpointsAreNotAccessibleWithWrongCredentials(String endpoint) {
    // ACT & ASSERT
    mockMvc
        .perform(get(endpoint).with(httpBasic("admin", "wrong-password")))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", matchesPattern("^Basic.*")));
  }
}
