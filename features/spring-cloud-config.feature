Feature: Spring Cloud Config Server Integration

  The _GitOps Config Server_ is built on top of the _Spring Cloud Config Server_, which provides a centralized configuration management solution for distributed systems. It allows you to manage application configuration across multiple environments and services, enabling dynamic updates without requiring application restarts.

  In the following scenarios, we will demonstrate how to retrieve configuration in different formats (YAML and JSON) and how to leverage variable substitution and profile-specific configurations. The configuration is stored in the [mihaly-farkas/gitops-config-server-example](https://github.com/mihaly-farkas/gitops-config-server-example) public GitHub repository

  Scenario: Get configuration in YAML format

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
    When I send a GET request to the "http://localhost:8888/config/v4/gitops_config_server-default.yaml" URL
    And  with basic HTTP authentication using the "admin" username and the "y0uR_S3cur3_aDm1N_P4ssw0rd" password
    Then the response status is "200 OK"
    And  the response is a valid YAML document
    And  the "name" field is equal to "GitOps Config Server Example"

  Scenario: Get configuration in JSON format

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
    When I send a GET request to the "http://localhost:8888/config/v4/gitops_config_server-default.json" URL
    And  with basic HTTP authentication using the "admin" username and the "y0uR_S3cur3_aDm1N_P4ssw0rd" password
    Then the response status is "200 OK"
    And  the response is a valid JSON object
    And  the "name" field is equal to "GitOps Config Server Example"

  Scenario: Variable Substitution in Configuration

  If you examine the configuration, you can see that the `name` property is referenced as a dynamic value:
  ```yaml
  name: GitOps Config Server Example
  description: Configuration for ${name}
  ```

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
    When I send a GET request to the "http://localhost:8888/config/v4/gitops_config_server-default.yaml" URL
    And  with basic HTTP authentication using the "admin" username and the "y0uR_S3cur3_aDm1N_P4ssw0rd" password
    Then the response status is "200 OK"
    And  the response is a valid YAML document
    And  the "name" field is equal to "GitOps Config Server Example"
    And  the "description" field is equal to "Configuration for GitOps Config Server Example"

  Scenario: Profile-Specific Configuration

  The _GitOps Config Server_ supports profile-specific configurations, allowing you to define different settings for various environments (e.g., testing, staging, production). You can specify the desired profile in the request URL to retrieve the corresponding configuration.

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
    When I send a GET request to the "http://localhost:8888/config/v4/gitops_config_server-overrides_by_profile.json" URL
    And  with basic HTTP authentication using the "admin" username and the "y0uR_S3cur3_aDm1N_P4ssw0rd" password
    Then the response status is "200 OK"
    And  the response is a valid JSON object
    And  the "name" field is equal to "GitOps Config Server Example (overridden by the 'overrides_by_profile' profile)"
    And  the "description" field is equal to "Configuration for GitOps Config Server Example (overridden by the 'overrides_by_profile' profile)"
