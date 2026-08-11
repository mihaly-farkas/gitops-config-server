package io.github.mihaly_farkas.spring_cloud_config_server;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@ActiveProfiles({"mockmvc"})
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
class ActuatorTest {

  static final String[] ACTUATOR_ENDPOINTS = {"/actuator", "/actuator/info", "/actuator/health"};

  @Autowired MockMvc mockMvc;

  @Value("${spring.security.user.name}")
  String securityUsername;

  @Value("${spring.security.user.password}")
  String securityPassword;

  static Stream<Arguments> actuatorEndpoints() {
    return Arrays.stream(ACTUATOR_ENDPOINTS).map(Arguments::of);
  }

  @ParameterizedTest
  @MethodSource("actuatorEndpoints")
  @DisplayName("The Spring Boot actuator endpoints are not accessible without authentication")
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
  @DisplayName("The Spring Boot actuator endpoints are not accessible with wrong credentials")
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
  @DisplayName("The Spring Boot actuator endpoints are accessible with correct credentials")
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
