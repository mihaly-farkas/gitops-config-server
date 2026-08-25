@since:v0.1.0
Feature: Docker

  @since:v0.1.0
  Scenario: Check health status of the container

  The distributed Docker image features a pre-configured, native HEALTHCHECK.

  This feature helps container runtimes and orchestration platforms evaluate the inner status of the container directly
  from the host environment, removing the dependency on external monitoring agents. By embedding this logic into the
  image layout, it establishes a reliable, self-contained contract that ensures containers are automatically restarted
  if they become unhealthy, leading to increased system resilience and lower operational maintenance.

  Key Benefits:
  - Zero Configuration: Eliminates the need for custom healthcheck commands, external tools, or custom scripts.
  - Orchestration Friendly: Enables automated, seamless startup sequencing in containerized environments.
  - Instant Readiness Checks: Guarantees that the container is fully operational and ready to serve configuration
  requests before dependent services boot up.

    Given Docker is running on my machine
    When  if the "spring-cloud-config-server.minimal" container is not running, I start a container with:
      """
      docker run \
        --name spring-cloud-config-server.minimal \
        --publish 8889:8888 \
        --detach \
        ghcr.io/mihaly-farkas/spring-cloud-config-server:local \
        --spring.cloud.config.server.git.uri='https://github.com/mihaly-farkas/spring-cloud-config-server-example' \
        --spring.cloud.config.server.git.default-label='main'
     """
    Then  the "spring-cloud-config-server.minimal" container is healthy
