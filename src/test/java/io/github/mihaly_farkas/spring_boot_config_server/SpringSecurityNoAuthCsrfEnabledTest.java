package io.github.mihaly_farkas.spring_boot_config_server;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"public_github_repo", "encrypt_key", "no_auth"})
class SpringSecurityNoAuthCsrfEnabledTest extends SpringSecurityTest {

  Stream<Arguments> postRequests() {
    return Stream.of(
        Arguments.of(
            configServerPrefix + "/encrypt",
            TEXT_PLAIN,
            "example body to send to the endpoint",
            OK,
            TEXT_PLAIN),
        Arguments.of(
            configServerPrefix + "/decrypt",
            TEXT_PLAIN,
            "99455f457f97d2786b3843224eebb63d749a45dee15f516295026d53e337a5e9",
            OK,
            TEXT_PLAIN));
  }

  @DisplayName("POST requests without CSRF token are rejected when CSRF is enabled")
  @MethodSource("postRequests")
  @ParameterizedTest(name = "endpoint -> {0}")
  @SneakyThrows
  void csrfTokensAreRequiredForPosts(String endpoint, MediaType contentType, String body) {
    // ACT & ASSERT
    mockMvc
        .perform(post(endpoint).contentType(contentType).content(body))
        .andExpect(status().isForbidden());
  }

  @DisplayName("POST requests with CSRF token are accepted when CSRF is enabled")
  @MethodSource("postRequests")
  @ParameterizedTest(name = "endpoint -> {0}")
  @SneakyThrows
  void csrfTokensAreAcceptedForPosts(
      String endpoint,
      MediaType contentType,
      String body,
      HttpStatus expectedHttpStatus,
      MediaType expectedContentType) {
    // ARRANGE
    var expectedStatus = expectedHttpStatus.value();

    // ACT & ASSERT
    mockMvc
        .perform(post(endpoint).with(csrf()).contentType(contentType).content(body))
        .andExpect(status().is(expectedStatus))
        .andExpect(content().contentTypeCompatibleWith(expectedContentType));
  }
}
