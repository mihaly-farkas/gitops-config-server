[![Continuous Delivery](https://github.com/mihaly-farkas/spring-cloud-config-server/actions/workflows/continuous-delivery.yaml/badge.svg)](https://github.com/mihaly-farkas/spring-cloud-config-server/actions/workflows/continuous-delivery.yaml)
[![CodeQL](https://github.com/mihaly-farkas/spring-cloud-config-server/actions/workflows/github-code-scanning/codeql/badge.svg)](https://github.com/mihaly-farkas/spring-cloud-config-server/actions/workflows/github-code-scanning/codeql)
[![Dependabot Updates](https://github.com/mihaly-farkas/spring-cloud-config-server/actions/workflows/dependabot/dependabot-updates/badge.svg)](https://github.com/mihaly-farkas/spring-cloud-config-server/actions/workflows/dependabot/dependabot-updates)

[![Quality gate status](https://sonarcloud.io/api/project_badges/measure?project=mihaly-farkas_spring-cloud-config-server&metric=alert_status&token=a83a9d01bf6b9e2e1a526f3c12a0bc1a1de4bbc9)](https://sonarcloud.io/summary/new_code?id=mihaly-farkas_spring-cloud-config-server)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=mihaly-farkas_spring-cloud-config-server&metric=coverage&token=a83a9d01bf6b9e2e1a526f3c12a0bc1a1de4bbc9)](https://sonarcloud.io/summary/new_code?id=mihaly-farkas_spring-cloud-config-server)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=mihaly-farkas_spring-cloud-config-server&metric=security_rating&token=a83a9d01bf6b9e2e1a526f3c12a0bc1a1de4bbc9)](https://sonarcloud.io/summary/new_code?id=mihaly-farkas_spring-cloud-config-server)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=mihaly-farkas_spring-cloud-config-server&metric=sqale_index&token=a83a9d01bf6b9e2e1a526f3c12a0bc1a1de4bbc9)](https://sonarcloud.io/summary/new_code?id=mihaly-farkas_spring-cloud-config-server)

[![Java 25 LTS](https://img.shields.io/badge/java-25%20LTS-blue.svg)](https://www.oracle.com/java/technologies/javase/jdk25-archive-downloads.html)
[![Maven 3.9.16](https://img.shields.io/badge/maven-3.9.16-blue.svg)](https://maven.apache.org/download.cgi)
[![Spring Boot 4.1.1](https://img.shields.io/badge/spring%20boot-4.1.1-blue.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud 2025.1.3](https://img.shields.io/badge/spring%20cloud-2025.1.3-blue.svg)](https://spring.io/projects/spring-cloud)
[![Dockerfile 1.26](https://img.shields.io/badge/dockerfile-1.26-blue.svg)](https://docs.docker.com/engine/reference/builder/)
[![dhi.io/eclipse-temurin](https://img.shields.io/badge/dhi.io%2Feclipse--temurin-25.0.4.7--alpine3.24-blue.svg)](https://hub.docker.com/hardened-images/catalog/dhi/eclipse-temurin/images/eclipse-temurin%2Falpine-3.24%2Fjre-25/sha256-1ea023a388d182af7407cc53549a4da244617f202486abd6a72adef75d5b1f59)

> _"Don't maintain configuration in five different places. Put them in a Git repo."_

# spring-cloud-config-server

Yet another containerized [Spring&nbsp;Cloud&nbsp;Config](https://spring.io/projects/spring-cloud-config) server.

## 📦 Features

- **Containerized Distribution:** Distributed as a Docker image with the required runtime configuration already assembled.

  - **Hardened Runtime Image:** Built on a security-hardened, minimal [Eclipse&nbsp;Temurin](https://hub.docker.com/hardened-images/catalog/dhi/eclipse-temurin/images?search=25-alpine&variants=runtime) runtime image designed to minimize the runtime attack surface, with no shell or unnecessary runtime tooling included.

- **Ready-to-Use Configuration Setup:** Comes with the essential Spring Boot, Spring Cloud Config, security, encryption, and operational configuration already set up.

  - **Built-in Security:** Provides setup support for Spring Security.

  - **Health and Monitoring:** Provides Spring Boot Actuator endpoints.

  - **OpenAPI Documentation:** Provides API documentation for the exposed configuration and management endpoints.

## 🚀 Quick Start

1. Run the container with the necessary property. For example, to consume configuration from this project's [example config repo](https://github.com/mihaly-farkas/spring-cloud-config-server-example) files, you can run:

   ```bash
    docker run \
      --name    spring-cloud-config-server \
      --publish 8888:8888 \
      ghcr.io/mihaly-farkas/spring-cloud-config-server:unstable \
      --spring.cloud.config.server.git.uri='https://github.com/mihaly-farkas/spring-cloud-config-server-example' \
      --encrypt.key='3x4mp13_r3p0_S3cur3_3Nc1pT1on_k3Y' \
      --spring.profiles.active='docker,no_auth'
   ```

   This example intentionally enables the `no_auth` profile to keep local evaluation simple.

2. Visit the Swagger UI for API exploration: http://localhost:8888/swagger-ui/index.html?urls.primaryName=Spring+Cloud+Config

## ⚠️ Disclaimer & Liability

This is a hobby project. I make no guarantee that it is production-ready. The project may contain experimental or incomplete features.

Use it at your own risk, and carefully review and adapt the configuration before using it in your own environment.

## ⚖️ License

This project is licensed under the [MIT License](https://github.com/mihaly-farkas/spring-cloud-config-server?tab=MIT-1-ov-file).
