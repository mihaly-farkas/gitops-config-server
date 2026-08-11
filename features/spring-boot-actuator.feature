@since:v0.1.0
Feature: Spring Boot Actuator

  _GitOps Config Server_ includes the _Spring Boot Actuator_, which provides production-ready features to help you
  monitor and manage your application.

  @since:v0.1.0
  Scenario: Open Actuator without credentials

  Default setup secures the application from unauthorized access and information leakage, as sensitive endpoints
  (such as `/env`, `/metrics`, or `/heapdump`) expose critical system details, environment variables, and internal
  configurations. By enforcing authentication out of the box, it ensures that only authorized administrators and
  automated internal tools can view or modify the application's runtime state.

    Given Docker is running on my machine
    When  if the "gitops-config-server.minimal" container is not running, I start a container with:
      """
      docker run \
        --name gitops-config-server.minimal \
        --publish 8889:8888 \
        --detach \
        ghcr.io/mihaly-farkas/gitops-config-server:feature-test \
        --spring.cloud.config.server.git.uri='https://github.com/mihaly-farkas/gitops-config-server-example' \
        --spring.cloud.config.server.git.default-label='main'
     """
    Then  the "gitops-config-server.minimal" container is healthy
    When I send a GET request to the "http://localhost:8889/actuator/v3" URL
    Then the response status is "200 OK"
    And  the response is an HTML document
    And  the HTML "title" is "Please sign in"

  @since:v0.1.0
  Scenario: Actuator health check endpoint without credentials

  The _Spring Boot Actuator_ health check endpoint is accessible without authorization.

  This feature helps orchestration tools (like Kubernetes) and monitoring systems (like Prometheus or AWS Route 53)
  constantly verify that the application is alive and responding, without needing complex authentication setups that
  could fail or leak credentials. It ensures high availability and automated self-healing in modern cloud environments
  while exposing only minimal, non-sensitive system status data.

    Given Docker is running on my machine
    When  if the "gitops-config-server.minimal" container is not running, I start a container with:
      """
      docker run \
        --name gitops-config-server.minimal \
        --publish 8889:8888 \
        --detach \
        ghcr.io/mihaly-farkas/gitops-config-server:feature-test \
        --spring.cloud.config.server.git.uri='https://github.com/mihaly-farkas/gitops-config-server-example' \
        --spring.cloud.config.server.git.default-label='main'
     """
    Then  the "gitops-config-server.minimal" container is healthy
    When I send a GET request to the "http://localhost:8889/actuator/v3/health" URL
    Then the response status is "200 OK"
    And  the response is a JSON object
    And  the "status" field is equal to "UP"

