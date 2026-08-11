package io.github.mihaly_farkas.gitops_config_server;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.json.JsonCompareMode.STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

@ActiveProfiles({"public_github_repo", "encrypt_key", "no_auth", "no_csrf"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpringCloudConfigServerEncryptionKeyTest extends MockMvcTest {

  @Value("${spring.cloud.config.server.prefix}")
  String configServerPrefix;

  Stream<Arguments> configs() {
    return Stream.of(
        Arguments.of(
            configServerPrefix + "/gitops_config_server-default.json",
            Map.of(
                "name",
                "GitOps Config Server Example",
                "description",
                "Configuration for GitOps Config Server Example",
                "secret",
                "secret value stored in encrypted form")),
        Arguments.of(
            configServerPrefix + "/gitops_config_server-overrides_by_profile.json",
            Map.of(
                "name",
                "GitOps Config Server Example (overridden by the 'overrides_by_profile' profile)",
                "description",
                "Configuration for GitOps Config Server Example (overridden by the 'overrides_by_profile' profile)",
                "secret",
                "secret value stored in encrypted form")));
  }

  @DisplayName(
      "Spring Cloud Config Server can resolve secret values when encryption key is configured")
  @MethodSource("configs")
  @ParameterizedTest(name = "endpoint -> {0}")
  @SneakyThrows
  void canResolveSecrets(String endpoint, Map<String, String> expectedValues) {
    // ACT & ASSERT
    mockMvc
        .perform(get(endpoint))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
        .andExpect(content().json(objectMapper.writeValueAsString(expectedValues), STRICT));
  }

  @DisplayName(
      "Spring Cloud Config Server encryption status is OK when encryption key is configured")
  @Test
  @SneakyThrows
  void encryptionStatusIsOk() {
    // ARRANGE
    var expectedResponse = objectMapper.writeValueAsString(Map.of("status", "OK"));

    // ACT & ASSERT
    mockMvc
        .perform(get(configServerPrefix + "/encrypt/status"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
        .andExpect(content().json(expectedResponse, STRICT));
  }

  @DisplayName(
      "Spring Cloud Config Server can encrypt secret value when encryption key is configured")
  @Test
  @SneakyThrows
  void canEncrypt() {
    // ARRANGE
    var plaintext = "my secret value";

    // ACT & ASSERT
    mockMvc
        .perform(post(configServerPrefix + "/encrypt").contentType(TEXT_PLAIN).content(plaintext))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(TEXT_PLAIN))
        .andExpect(content().string(not(emptyString())))
        .andExpect(content().string(not(equalTo(plaintext))));
  }

  @DisplayName(
      "Spring Cloud Config Server can decrypt secret value when encryption key is configured")
  @Test
  @SneakyThrows
  void canDecrypt() {
    // ARRANGE
    var plaintext = "my secret value";
    var ciphertext = "40b37c83566707fb61ddb2e7c920ec62296f48e881ca8e801c124fb19d4cb4f3";

    // ACT & ASSERT
    mockMvc
        .perform(post(configServerPrefix + "/decrypt").contentType(TEXT_PLAIN).content(ciphertext))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(TEXT_PLAIN))
        .andExpect(content().string(plaintext));
  }
}
