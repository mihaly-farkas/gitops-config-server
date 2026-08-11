package io.github.mihaly_farkas.gitops_config_server;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.json.JsonCompareMode.LENIENT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles({"mockmvc", "mockmvc_encrypt_key"})
@AutoConfigureMockMvc
class ConfigEncryptKeyTest {

  @Autowired MockMvc mockMvc;

  @Autowired ObjectMapper objectMapper;

  @Value("${spring.security.user.name}")
  String username;

  @Value("${spring.security.user.password}")
  String password;

  static Stream<Arguments> configTestCases() {
    return Stream.of(
        Arguments.of(
            "/config/v4/gitops_config_server-default.json",
            Map.of(
                "name", "GitOps Config Server Example",
                "description", "Configuration for GitOps Config Server Example",
                "secret", "secret value stored in encrypted form")),
        Arguments.of(
            "/config/v4/gitops_config_server-overrides_by_profile.json",
            Map.of(
                "name",
                    "GitOps Config Server Example (overridden by the 'overrides_by_profile' profile)",
                "description",
                    "Configuration for GitOps Config Server Example (overridden by the 'overrides_by_profile' profile)",
                "secret", "secret value stored in encrypted form")));
  }

  @ParameterizedTest
  @MethodSource("configTestCases")
  @DisplayName("The GitOps Config Server returns the expected configuration values")
  @SneakyThrows
  void springCloudConfigServerReturnsExpectedConfigurationValues(
      String endpoint, Map<String, String> expectedValues) {
    // ACT & ASSERT
    mockMvc
        .perform(get(endpoint).with(httpBasic(username, password)))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
        .andExpect(content().json(objectMapper.writeValueAsString(expectedValues), LENIENT));
  }

  @Test
  @DisplayName("The GitOps Config Server's encryption status is OK")
  @SneakyThrows
  void springCloudConfigServerEncryptionStatusIsOk() {
    // ACT & ASSERT
    mockMvc
        .perform(get("/config/v4/encrypt/status").with(httpBasic(username, password)))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
        .andExpect(
            content().json(objectMapper.writeValueAsString(Map.of("status", "OK")), LENIENT));
  }

  @Test
  @DisplayName("The GitOps Config Server can encrypt and decrypt a given plaintext value")
  @SneakyThrows
  void springCloudConfigServerCanEncryptAndDecryptGivenPlaintext() {
    // ARRANGE
    var plaintext = "my secret value";

    // ACT & ASSERT
    var encryptResult =
        mockMvc
            .perform(
                post("/config/v4/encrypt")
                    .with(httpBasic(username, password))
                    .contentType(TEXT_PLAIN)
                    .content(plaintext))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(TEXT_PLAIN))
            .andExpect(content().string(not(emptyString())))
            .andExpect(content().string(not(equalTo(plaintext))))
            .andReturn();

    // ARRANGE
    var ciphertext = encryptResult.getResponse().getContentAsString();

    // ACT & ASSERT
    mockMvc
        .perform(
            post("/config/v4/decrypt")
                .with(httpBasic(username, password))
                .contentType(TEXT_PLAIN)
                .content(ciphertext))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(TEXT_PLAIN))
        .andExpect(content().string(plaintext));
  }
}
