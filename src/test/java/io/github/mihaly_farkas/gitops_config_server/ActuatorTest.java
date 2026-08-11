package io.github.mihaly_farkas.gitops_config_server;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles({"mockmvc"})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ActuatorTest {

  @Autowired MockMvc mockMvc;

  @Autowired WebEndpointsSupplier webEndpointsSupplier;

  @Value("${spring.security.user.name}")
  String securityUsername;

  @Value("${spring.security.user.password}")
  String securityPassword;

  Stream<Arguments> actuatorEndpoints() {
    return Stream.concat(
        Stream.of(Arguments.of("/actuator")),
        webEndpointsSupplier.getEndpoints().stream()
            .map(PathMappedEndpoint::getRootPath)
            .map(rootPath -> "/actuator/" + rootPath)
            .map(Arguments::of));
  }

  @ParameterizedTest
  @MethodSource("actuatorEndpoints")
  @DisplayName("The Spring Boot Actuator endpoints are not accessible without authentication")
  @SneakyThrows
  void springBootActuatorEndpointsAreNotAccessibleWithoutAuthentication(String endpoint) {
    // ACT & ASSERT
    mockMvc
        .perform(get(endpoint))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", matchesPattern("^Basic.*")));
  }

  @ParameterizedTest
  @MethodSource("actuatorEndpoints")
  @DisplayName("The Spring Boot Actuator endpoints are not accessible with wrong credentials")
  @SneakyThrows
  void springBootActuatorEndpointsAreNotAccessibleWithWrongCredentials(String endpoint) {
    // ACT & ASSERT
    mockMvc
        .perform(get(endpoint).with(httpBasic(securityUsername, "wrong-password")))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", matchesPattern("^Basic.*")));
  }

  @ParameterizedTest
  @MethodSource("actuatorEndpoints")
  @DisplayName("The Spring Boot Actuator endpoints are accessible with correct credentials")
  @SneakyThrows
  void springBootActuatorEndpointsAreAccessibleWithCorrectCredentials(String endpoint) {
    // ACT & ASSERT
    mockMvc
        .perform(get(endpoint).with(httpBasic(securityUsername, securityPassword)))
        .andExpect(status().isOk())
        .andExpect(
            content().contentTypeCompatibleWith("application/vnd.spring-boot.actuator.v3+json"));
  }
}
