Feature: Built-in Container Healthcheck

  The distributed _Docker_ image features a **pre-configured, native HEALTHCHECK**. This provides a canonical standard for container state management, allowing both DevOps engineers and orchestration tools (such as _Docker Compose_) to reliably determine readiness.

  **Key Benefits:**

  - **Zero Configuration:** Eliminates the need for custom healthcheck commands, external tools, or custom scripts.

  - **Orchestration Friendly:** Enables automated, seamless startup sequencing in containerized environments.

  - **Instant Readiness Checks:** Guarantees that the _GitOps Config Server_ is fully operational and ready to serve configuration requests before dependent services boot up.

  Scenario: Checking the Health of the GitOps Config Server Container

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
    Then  the "gitops-config-server.minimal-config" container should have a healthcheck configured
    And   the "gitops-config-server.minimal-config" container should be healthy
