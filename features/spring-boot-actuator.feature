Feature: Spring Boot Actuator

  _GitOps Config Server_ includes the _Spring Boot Actuator_ module, which provides production-ready features to help you monitor and manage your application. The actuator exposes various endpoints that allow you to check the application's health, metrics, and other operational information.

  Scenario: Health Check Endpoint

  By default, the _GitOps Config Server_ exposes the _Spring Boot Actuator_ health check endpoint at `/actuator/health`.

  **Environment variables for the Docker container:**
  - `CONFIG_GIT_URI`: The URI of the Git repository containing the configuration files.
  - `CONFIG_ADMIN_PASSWORD`: The password for the admin user to access the _GitOps Config Server_.

    Given Docker is running on my machine
    When  if the "gitops-config-server.minimal-config" container is not running, I run the following command:
      """
        docker run \
          --name    gitops-config-server.minimal-config \
          --env     CONFIG_GIT_URI=https://github.com/mihaly-farkas/gitops-config-server-example \
          --env     CONFIG_ADMIN_PASSWORD="y0uR_S3cur3_aDm1N_P4ssw0rd" \
          --publish 8888:8888 \
          --detach \
          ghcr.io/mihaly-farkas/gitops-config-server:feature-test
      """
    And  I wait until the "gitops-config-server.minimal-config" container is healthy
    When I send a GET request to the "http://localhost:8888/actuator/health" URL
    And  with basic HTTP authentication using the "admin" username and the "y0uR_S3cur3_aDm1N_P4ssw0rd" password
    Then the response status is "200 OK"
    And  the response is a valid JSON object
    And  the "status" field is equal to "UP"

