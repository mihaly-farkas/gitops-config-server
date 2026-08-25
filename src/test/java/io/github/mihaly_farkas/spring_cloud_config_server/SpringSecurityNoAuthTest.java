package io.github.mihaly_farkas.spring_cloud_config_server;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
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

@ActiveProfiles({"public_github_repo", "all_actuator_endpoints", "no_auth"})
class SpringSecurityNoAuthTest extends SpringSecurityTest {

  @DisplayName(
      "All endpoints are accessible without authentication when Spring Security is disabled")
  @MethodSource("allEndpoints")
  @ParameterizedTest(name = "endpoint -> {0}")
  @SneakyThrows
  void endpointsAreAccessibleWithoutAuth(
      String endpoint, HttpStatus expectedHttpStatus, MediaType expectedContentType) {
    // ARRANGE
    var expectedStatus = expectedHttpStatus.value();

    // ACT & ASSERT
    mockMvc
        .perform(get(endpoint))
        .andExpect(status().is(expectedStatus))
        .andExpect(
            expectedContentType != null
                ? content().contentTypeCompatibleWith(expectedContentType)
                : header().doesNotExist(CONTENT_TYPE));
  }
}
