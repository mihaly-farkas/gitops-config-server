package io.github.mihaly_farkas.gitops_config_server;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.json.JsonCompareMode.STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.ServletException;
import java.util.Map;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@ActiveProfiles({"public_github_repo", "no_auth", "no_csrf"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpringCloudConfigServerNoEncryptionTest extends MockMvcTest {

  @Value("${spring.cloud.config.server.prefix}")
  String configServerPrefix;

  Stream<Arguments> configs() {
    return Stream.of(
        Arguments.of(
            configServerPrefix + "/gitops_config_server-default.json",
            Map.of(
                "name", "GitOps Config Server Example",
                "description", "Configuration for GitOps Config Server Example",
                "invalid", Map.of("secret", "<n/a>"))),
        Arguments.of(
            configServerPrefix + "/gitops_config_server-overrides_by_profile.json",
            Map.of(
                "name",
                "GitOps Config Server Example (overridden by the 'overrides_by_profile' profile)",
                "description",
                "Configuration for GitOps Config Server Example (overridden by the 'overrides_by_profile' profile)",
                "invalid",
                Map.of("secret", "<n/a>"))));
  }

  @DisplayName(
      "Spring Cloud Config Server can NOT resolve secrets when encryption key is not configured")
  @MethodSource("configs")
  @ParameterizedTest(name = "endpoint -> {0}")
  @SneakyThrows
  void canNotResolveSecrets(String endpoint, Map<String, Object> expectedValues) {
    // ACT & ASSERT
    mockMvc
        .perform(get(endpoint))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
        .andExpect(content().json(objectMapper.writeValueAsString(expectedValues), STRICT));
  }

  @DisplayName(
      "Spring Cloud Config Server endpoint '/encrypt/status' returns with error when encryption is not configured")
  @Test
  void encryptStatusReturnsServerError() {
    // ACT & ASSERT
    assertUnsupportedOperationError(get(configServerPrefix + "/encrypt/status"));
  }

  @DisplayName(
      "Spring Cloud Config Server endpoint '/encrypt' returns with error when encryption is not configured")
  @Test
  void encryptReturnsServerError() {
    // ARRANGE
    var plaintext = "my secret value";

    // ACT & ASSERT
    assertUnsupportedOperationError(
        post(configServerPrefix + "/encrypt").contentType(TEXT_PLAIN).content(plaintext));
  }

  @DisplayName(
      "Spring Cloud Config Server endpoint '/decrypt' returns with error when encryption is not configured")
  @Test
  void decryptReturnsServerError() {
    // ARRANGE
    var ciphertext =
        "af233f130ad751b1f39bcd4db1a37ca887ee6109d47353e7631a684ce793a3111dda06538ac9b4e15f0df0530b282d91e623de0feddba"
            + "eaf7111ff0b1f0e3cda";

    // ACT & ASSERT
    assertUnsupportedOperationError(
        post(configServerPrefix + "/decrypt").contentType(TEXT_PLAIN).content(ciphertext));
  }

  @SneakyThrows
  void assertUnsupportedOperationError(MockHttpServletRequestBuilder requestBuilder) {
    try {
      mockMvc.perform(requestBuilder).andExpect(status().is5xxServerError());
    } catch (ServletException exception) {
      assertThat(exception.getCause(), instanceOf(UnsupportedOperationException.class));
      assertThat(exception.getCause().getMessage(), containsString("No encryption"));
    }
  }
}
