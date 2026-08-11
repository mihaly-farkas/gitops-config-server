package io.github.mihaly_farkas.gitops_config_server;

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
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({
  "mockmvc_public_github_repo",
  "mockmvc_all_actuator_endpoints",
  "disable_spring_security"
})
class SpringSecurityDisabledTest extends SpringSecurityTest {

  @ParameterizedTest(name = "endpoint -> {0}")
  @MethodSource("allEndpoints")
  @DisplayName(
      "When Spring Security is disabled, ALL endpoints are accessible without authorization")
  @SneakyThrows
  void endpointIsAccessibleWithoutAuth(String endpoint, String expectedContentType) {
    // ARRANGE
    var springSecurityUsername = springSecurityUsername();
    var springSecurityPassword = springSecurityPassword();

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
}
