package io.github.mihaly_farkas.gitops_config_server;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_HTML;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles({"mockmvc_public_github_repo", "disable_spring_security"})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpringDocTest {

  @Autowired MockMvc mockMvc;

  @Value("${springdoc.api-docs.path:/v3/api-docs}")
  String apiDocsPath;

  @Value("${springdoc.swagger-ui.path:/swagger-ui.html}")
  String swaggerUiPath;

  Stream<Arguments> swaggerUiEndpoints() {
    return Stream.of(Arguments.of(apiDocsPath), Arguments.of(swaggerUiPath));
  }

  @Test
  @DisplayName("SpringDoc provides OpenAPI specification")
  @SneakyThrows
  void apiDocProvidesOpenApiSpecification() {
    // ACT & ASSERT
    mockMvc
        .perform(get(apiDocsPath))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
        .andExpect(jsonPath("$.openapi").value(matchesPattern("^3\\.\\d+\\.\\d+$")));
  }

  @Test
  @DisplayName("SpringDoc Swagger redirects to Swagger UI index.html page")
  @SneakyThrows
  void swaggerRedirectsToIndex() {
    // ARRANGE
    var swaggerIndexHtml = swaggerUiPath.replaceAll("\\.html$", "") + "/index.html";

    // ACT & ASSERT
    mockMvc
        .perform(get(swaggerUiPath))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(swaggerIndexHtml));
  }

  @Test
  @DisplayName("SpringDoc Swagger UI index.html is a valid HTML page")
  @SneakyThrows
  void swaggerUiIndexIsHtml() {
    // ARRANGE
    var swaggerIndexHtml = swaggerUiPath.replaceAll("\\.html$", "") + "/index.html";

    // ACT & ASSERT
    mockMvc
        .perform(get(swaggerIndexHtml).header(ACCEPT, TEXT_HTML))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(TEXT_HTML));
  }
}
