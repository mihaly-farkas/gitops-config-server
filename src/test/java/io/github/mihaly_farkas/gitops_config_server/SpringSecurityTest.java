package io.github.mihaly_farkas.gitops_config_server;

import static java.util.Objects.requireNonNull;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_HTML_VALUE;

import java.util.Map;
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
abstract class SpringSecurityTest extends MockmvcTest {

  @Autowired WebEndpointsSupplier webEndpointsSupplier;

  @Autowired InMemoryUserDetailsManager userDetailsManager;

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
    return Stream.concat(protectedEndpoints(), openEndpoints());
  }

  Stream<Arguments> protectedEndpoints() {
    return Stream.concat(
        actuatorProtectedEndpoints(),
        Stream.concat(springCloudConfigServerEndpoints(), springDocEndpoints()));
  }

  Stream<Arguments> openEndpoints() {
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
        .map(path -> Arguments.of(path, "application/vnd.spring-boot.actuator.v3+json"));
  }

  Stream<Arguments> springCloudConfigServerEndpoints() {
    return Stream.of(
        Arguments.of(configServerPrefix, APPLICATION_PROBLEM_JSON_VALUE),
        Arguments.of(
            configServerPrefix + "/gitops_config_server-default.json", APPLICATION_JSON_VALUE),
        Arguments.of(
            configServerPrefix + "/gitops_config_server-overrides_by_profile.json",
            APPLICATION_JSON_VALUE));
  }

  Stream<Arguments> springDocEndpoints() {
    return Stream.of(
        Arguments.of(apiDocsPath, APPLICATION_JSON_VALUE),
        Arguments.of(swaggerUiPath, null),
        Arguments.of(swaggerUiPath.replaceAll("\\.html$", "") + "/index.html", TEXT_HTML_VALUE));
  }

  @SuppressWarnings("unchecked")
  UserDetails springSecurityUser() throws NoSuchFieldException, IllegalAccessException {
    var usersField = InMemoryUserDetailsManager.class.getDeclaredField("users");
    usersField.setAccessible(true);
    var users = (Map<String, UserDetails>) usersField.get(userDetailsManager);
    return users.get("user");
  }

  String springSecurityUsername() throws NoSuchFieldException, IllegalAccessException {
    return requireNonNull(springSecurityUser().getUsername());
  }

  String springSecurityPassword() throws NoSuchFieldException, IllegalAccessException {
    if (springSecurityPassword == null) {
      springSecurityPassword =
          requireNonNull(springSecurityUser().getPassword()).replace("{noop}", "");
    }
    return requireNonNull(springSecurityPassword);
  }
}
