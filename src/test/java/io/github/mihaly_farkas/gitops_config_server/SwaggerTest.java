package io.github.mihaly_farkas.gitops_config_server;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_HTML;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles({"mockmvc"})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SwaggerTest {

  @Autowired MockMvc mockMvc;

  @Value("${spring.security.user.name}")
  String securityUsername;

  @Value("${spring.security.user.password}")
  String securityPassword;

  @Value("${springdoc.api-docs.path:/v3/api-docs}")
  String apiDocsPath;

  @Value("${springdoc.swagger-ui.path:/swagger-ui.html}")
  String swaggerUiPath;

  Stream<Arguments> swaggerUiEndpoints() {
    return Stream.of(Arguments.of(apiDocsPath), Arguments.of(swaggerUiPath));
  }

  @ParameterizedTest
  @MethodSource("swaggerUiEndpoints")
  @DisplayName("The SpringDoc endpoints are not accessible without authentication")
  @SneakyThrows
  void springDocEndpointsAreNotAccessibleWithoutAuthentication(String endpoint) {
    // ACT & ASSERT
    mockMvc
        .perform(get(endpoint))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", matchesPattern("^Basic.*")));
  }

  @ParameterizedTest
  @MethodSource("swaggerUiEndpoints")
  @DisplayName("The SpringDoc endpoints are not accessible with wrong credentials")
  @SneakyThrows
  void springDocEndpointsAreNotAccessibleWithWrongCredentials(String endpoint) {
    // ACT & ASSERT
    mockMvc
        .perform(get(endpoint).with(httpBasic(securityUsername, "wrong-password")))
        .andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @MethodSource("swaggerUiEndpoints")
  @DisplayName("The SpringDoc endpoints are accessible with correct credentials")
  @SneakyThrows
  void springDocEndpointsAreAccessibleWithCorrectCredentials(String endpoint) {
    // ACT & ASSERT
    mockMvc
        .perform(get(endpoint).with(httpBasic(securityUsername, securityPassword)))
        .andExpect(status().is(anyOf(is(HttpStatus.OK.value()), is(HttpStatus.FOUND.value()))));
  }

  @Test
  @DisplayName("The SpringDoc API docs returns a valid OpenAPI specification")
  @SneakyThrows
  void springDocApiDocsReturnsValidOpenApiSpecification() {
    // ACT & ASSERT
    mockMvc
        .perform(get(apiDocsPath).with(httpBasic(securityUsername, securityPassword)))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
        .andExpect(jsonPath("$.openapi").value(matchesPattern("^3\\.\\d+\\.\\d+$")));
  }

  @Test
  @DisplayName("The SpringDoc Swagger UI path redirects to the Swagger UI index.html page")
  @SneakyThrows
  void springDocSwaggerUiRedirectsToIndexHtml() {
    // ARRANGE
    var swaggerIndexHtml = swaggerUiPath.replaceAll("\\.html$", "") + "/index.html";

    // ACT & ASSERT
    mockMvc
        .perform(get(swaggerUiPath).with(httpBasic(securityUsername, securityPassword)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(swaggerIndexHtml));
  }

  @Test
  @DisplayName("The SpringDoc Swagger UI index.html page is a valid HTML page")
  @SneakyThrows
  void springDocSwaggerUiIndexHtmlIsValidHtml() {
    // ARRANGE
    var swaggerIndexHtml = swaggerUiPath.replaceAll("\\.html$", "") + "/index.html";

    // ACT & ASSERT
    mockMvc
        .perform(
            get(swaggerIndexHtml)
                .with(httpBasic(securityUsername, securityPassword))
                .header("Accept", "text/html"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(TEXT_HTML));
  }
}
