package io.github.mihaly_farkas.gitops_config_server;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
@ActiveProfiles({"mockmvc_public_github_repo", "disable_spring_security"})
@AutoConfigureMockMvc
class SpringCloudConfigServerNoEncryptionTest {

  @Autowired MockMvc mockMvc;

  @Autowired ObjectMapper objectMapper;

  @Value("${spring.cloud.config.server.prefix}")
  String configServerPrefix;

  static Stream<Arguments> configs() {
    return Stream.of(
        Arguments.of(
            "/config/v4/gitops_config_server-default.json",
            Map.of(
                "name", "GitOps Config Server Example",
                "description", "Configuration for GitOps Config Server Example",
                "invalid", Map.of("secret", "<n/a>"))),
        Arguments.of(
            "/config/v4/gitops_config_server-overrides_by_profile.json",
            Map.of(
                "name",
                "GitOps Config Server Example (overridden by the 'overrides_by_profile' profile)",
                "description",
                "Configuration for GitOps Config Server Example (overridden by the 'overrides_by_profile' profile)",
                "invalid",
                Map.of("secret", "<n/a>"))));
  }

  @ParameterizedTest(name = "endpoint -> {0}")
  @MethodSource("configs")
  @DisplayName(
      "When encryption is NOT configured,"
          + " Spring Cloud Config Server CAN NOT resolve secret values")
  @SneakyThrows
  void canNotResolveSecrets(String endpoint, Map<String, Object> expectedValues) {
    // ACT & ASSERT
    mockMvc
        .perform(get(endpoint))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
        .andExpect(content().json(objectMapper.writeValueAsString(expectedValues), STRICT));
  }

  @Test
  @DisplayName(
      "When encryption is NOT configured,"
          + " Spring Cloud Config Server encrypt status"
          + " throws an exception")
  void encryptStatusThrowsException() {
    // ACT & ASSERT
    assertThrows(
        ServletException.class, () -> mockMvc.perform(get(configServerPrefix + "/encrypt/status")));
  }

  @Test
  @DisplayName(
      "When encryption is NOT configured,"
          + " Spring Cloud Config Server encrypt"
          + " throws an exception")
  @SneakyThrows
  void encryptThrowsException() {
    // ARRANGE
    var plaintext = "my secret value";

    // ACT & ASSERT
    assertThrows(
        ServletException.class,
        () ->
            mockMvc.perform(
                post(configServerPrefix + "/encrypt").contentType(TEXT_PLAIN).content(plaintext)));
  }

  @Test
  @DisplayName(
      "When encryption is NOT configured,"
          + " Spring Cloud Config Server decrypt"
          + " throws an exception")
  @SneakyThrows
  void decryptThrowsException() {
    // ARRANGE
    var ciphertext =
        "af233f130ad751b1f39bcd4db1a37ca887ee6109d47353e7631a684ce793a3111dda06538ac9b4e15f0df0530b282d91e623de0feddba"
            + "eaf7111ff0b1f0e3cda";

    // ACT & ASSERT
    assertThrows(
        ServletException.class,
        () ->
            mockMvc.perform(
                post(configServerPrefix + "/decrypt").contentType(TEXT_PLAIN).content(ciphertext)));
  }
}
