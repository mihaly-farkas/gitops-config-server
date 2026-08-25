package io.github.mihaly_farkas.spring_cloud_config_server;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_HTML;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"public_github_repo", "no_auth"})
class SpringDocTest extends MockMvcTest {

  @Value("${springdoc.api-docs.path:/v3/api-docs}")
  String apiDocsPath;

  @Value("${springdoc.swagger-ui.path:/swagger-ui.html}")
  String swaggerUiPath;

  @DisplayName("SpringDoc provides OpenAPI specification")
  @Test
  @SneakyThrows
  void apiDocProvidesOpenApiSpecification() {
    // ACT & ASSERT
    mockMvc
        .perform(get(apiDocsPath))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
        .andExpect(jsonPath("$.openapi").value(matchesPattern("^3\\.\\d+\\.\\d+$")));
  }

  @DisplayName("SpringDoc default Swagger path redirects to index.html")
  @Test
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

  @DisplayName("SpringDoc Swagger index.html is a valid HTML page")
  @Test
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
