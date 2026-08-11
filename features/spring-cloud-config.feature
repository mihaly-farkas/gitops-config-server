@since:v0.1.0
Feature: Spring Cloud Config Server

  _GitOps Config Server_ is built on top of _Spring Cloud Config Server_, which provides a centralized configuration
  management solution for distributed systems. It allows you to manage application configuration across multiple
  environments and services, enabling dynamic updates without requiring application restarts.

  In the following scenarios, we will demonstrate how to retrieve configuration in different formats (YAML and JSON)
  and how to leverage variable substitution and profile-specific configurations. The configuration is stored in the
  [mihaly-farkas/gitops-config-server-example](https://github.com/mihaly-farkas/gitops-config-server-example) public
  GitHub repository

  @since:v0.1.0
  Scenario: Get configuration in YAML format

    Given Docker is running on my machine
    When  if the "gitops-config-server.encrypt-key" container is not running, I start a container with:
      """
      docker run \
        --name gitops-config-server.encrypt-key \
        --publish 8890:8888 \
        --detach \
        ghcr.io/mihaly-farkas/gitops-config-server:feature-test \
        --spring.profiles.active='docker,disable_spring_security' \
        --spring.cloud.config.server.git.uri='https://github.com/mihaly-farkas/gitops-config-server-example' \
        --spring.cloud.config.server.git.default-label='main' \
        --encrypt.key='3x4mp13_r3p0_S3cur3_3Nc1pT1on_k3Y'
     """
    Then the "gitops-config-server.encrypt-key" container is healthy
    When I send a GET request to the "http://localhost:8890/config/v4/gitops_config_server-default.yaml" URL
    Then the response status is "200 OK"
    And  the response is a YAML document
    And  the "name" field is equal to "GitOps Config Server Example"
    And  the "description" field is equal to "Configuration for GitOps Config Server Example"
    And  the "secret" field is equal to "secret value stored in encrypted form"

