package io.github.mihaly_farkas.spring_cloud_config_server;

import static io.github.mihaly_farkas.spring_cloud_config_server.lib.http.MediaType.APPLICATION_SPRING_CLOUD_ACTUATOR_JSON;
import static java.util.Objects.requireNonNull;
import static org.springframework.http.HttpStatus.FOUND;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.http.MediaType.TEXT_HTML;

import java.util.stream.Stream;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class SpringSecurityTest extends MockMvcTest {

  @Autowired WebEndpointsSupplier webEndpointsSupplier;

  @Autowired InMemoryUserDetailsManager userDetailsManager;

  @Value("${spring.security.user.name:user}")
  String springSecurityUsername;

  @Value("${management.endpoints.web.base-path:/actuator}")
  String actuatorBasePath;

  @Value("${spring.cloud.config.server.prefix}")
  String configServerPrefix;

  @Value("${springdoc.api-docs.path:/v3/api-docs}")
  String apiDocsPath;

  @Value("${springdoc.swagger-ui.path:/swagger-ui.html}")
  String swaggerUiPath;

  private String springSecurityPassword;

  Stream<Arguments> allEndpoints() {
    return Stream.concat(protectedEndpoints(), unprotectedEndpoints());
  }

  Stream<Arguments> protectedEndpoints() {
    return Stream.concat(
        actuatorProtectedEndpoints(),
        Stream.concat(springCloudConfigServerEndpoints(), springDocEndpoints()));
  }

  Stream<Arguments> unprotectedEndpoints() {
    return actuatorOpenEndpoints();
  }

  Stream<Arguments> actuatorProtectedEndpoints() {
    return actuatorEndpoints()
        .filter(arguments -> !(actuatorBasePath + "/health").equals(arguments.get()[0]));
  }

  Stream<Arguments> actuatorOpenEndpoints() {
    return actuatorEndpoints()
        .filter(arguments -> (actuatorBasePath + "/health").equals(arguments.get()[0]));
  }

  Stream<Arguments> actuatorEndpoints() {
    return Stream.concat(
            Stream.of(actuatorBasePath),
            webEndpointsSupplier.getEndpoints().stream()
                .map(PathMappedEndpoint::getRootPath)
                .map(rootPath -> actuatorBasePath + "/" + rootPath))
        .map(path -> Arguments.of(path, OK, APPLICATION_SPRING_CLOUD_ACTUATOR_JSON));
  }

  Stream<Arguments> springCloudConfigServerEndpoints() {
    return Stream.of(
        Arguments.of(configServerPrefix, NOT_FOUND, APPLICATION_PROBLEM_JSON),
        Arguments.of(
            configServerPrefix + "/spring_cloud_config_server-default.json", OK, APPLICATION_JSON),
        Arguments.of(
            configServerPrefix + "/spring_cloud_config_server-overrides_by_profile.json",
            OK,
            APPLICATION_JSON));
  }

  Stream<Arguments> springDocEndpoints() {
    return Stream.of(
        Arguments.of(apiDocsPath, OK, APPLICATION_JSON),
        Arguments.of(swaggerUiPath, FOUND, null),
        Arguments.of(swaggerUiPath.replaceAll("\\.html$", "") + "/index.html", OK, TEXT_HTML));
  }

  UserDetails springSecurityUser() {
    return userDetailsManager.loadUserByUsername(springSecurityUsername);
  }

  String springSecurityUsername() {
    return requireNonNull(springSecurityUser().getUsername());
  }

  String springSecurityPassword() {
    if (springSecurityPassword == null) {
      springSecurityPassword =
          requireNonNull(springSecurityUser().getPassword()).replace("{noop}", "");
    }
    return requireNonNull(springSecurityPassword);
  }
}
