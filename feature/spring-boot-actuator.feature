@since:v0.1.0
Feature: Spring Boot Actuator

  _Spring Boot Config Server_ includes the _Spring Boot Actuator_, which provides production-ready features to help you
  monitor and manage your application.

  @since:v0.1.0
  Scenario: Actuator health check endpoint without credentials

  The _Spring Boot Actuator_ health check endpoint is accessible without authorization.

  This feature helps orchestration tools (like Kubernetes) and monitoring systems (like Prometheus or AWS Route 53)
  constantly verify that the application is alive and responding, without needing complex authentication setups that
  could fail or leak credentials. It ensures high availability and automated self-healing in modern cloud environments
  while exposing only minimal, non-sensitive system status data.

    Given Docker is running on my machine
    When  if the "spring-boot-config-server.minimal" container is not running, I start a container with:
      """
      docker run \
        --name spring-boot-config-server.minimal \
        --publish 8889:8888 \
        --detach \
        ghcr.io/mihaly-farkas/spring-boot-config-server:local \
        --spring.cloud.config.server.git.uri='https://github.com/mihaly-farkas/spring-boot-config-server-example' \
        --spring.cloud.config.server.git.default-label='main'
     """
    Then  the "spring-boot-config-server.minimal" container is healthy
    When I send a GET request to the "http://localhost:8889/actuator/health" URL
    Then the response status is "200 OK"
    And  the response is a JSON object
    And  the "status" field is equal to "UP"

