[![Continuous Delivery](https://github.com/mihaly-farkas/spring-cloud-config-server/actions/workflows/continuous-delivery.yaml/badge.svg)](https://github.com/mihaly-farkas/spring-cloud-config-server/actions/workflows/continuous-delivery.yaml)
[![CodeQL](https://github.com/mihaly-farkas/spring-cloud-config-server/actions/workflows/github-code-scanning/codeql/badge.svg)](https://github.com/mihaly-farkas/spring-cloud-config-server/actions/workflows/github-code-scanning/codeql)
[![Dependabot Updates](https://github.com/mihaly-farkas/spring-cloud-config-server/actions/workflows/dependabot/dependabot-updates/badge.svg)](https://github.com/mihaly-farkas/spring-cloud-config-server/actions/workflows/dependabot/dependabot-updates)

[![Sonar quality gate status](https://sonarcloud.io/api/project_badges/measure?project=mihaly-farkas_spring-boot-config-server&metric=alert_status&token=d45a54717c09a5b9b40a9a8d07214aad8d92c9dc)](https://sonarcloud.io/summary/new_code?id=mihaly-farkas_spring-boot-config-server)
[![Sonar coverage](https://sonarcloud.io/api/project_badges/measure?project=mihaly-farkas_spring-boot-config-server&metric=coverage&token=d45a54717c09a5b9b40a9a8d07214aad8d92c9dc)](https://sonarcloud.io/summary/new_code?id=mihaly-farkas_spring-boot-config-server)
[![Sonar Security Rating](https://sonarcloud.io/api/project_badges/measure?project=mihaly-farkas_spring-boot-config-server&metric=security_rating&token=d45a54717c09a5b9b40a9a8d07214aad8d92c9dc)](https://sonarcloud.io/summary/new_code?id=mihaly-farkas_spring-boot-config-server)
[![Sonar Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=mihaly-farkas_spring-boot-config-server&metric=sqale_index&token=d45a54717c09a5b9b40a9a8d07214aad8d92c9dc)](https://sonarcloud.io/summary/new_code?id=mihaly-farkas_spring-boot-config-server)

[![Java 25 LTS](https://img.shields.io/badge/java-25%20LTS-blue.svg)](https://www.oracle.com/java/technologies/javase/jdk25-archive-downloads.html)
[![Maven 3.9.16](https://img.shields.io/badge/maven-3.9.16-blue.svg)](https://maven.apache.org/download.cgi)
[![Spring Boot 4.1.1](https://img.shields.io/badge/spring%20boot-4.1.1-blue.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud 2025.1.3](https://img.shields.io/badge/spring%20cloud-2025.1.3-blue.svg)](https://spring.io/projects/spring-cloud)
[![Dockerfile 1.26](https://img.shields.io/badge/dockerfile-1.26-blue.svg)](https://docs.docker.com/engine/reference/builder/)
[![dhi.io/eclipse-temurin](https://img.shields.io/badge/dhi.io%2Feclipse--temurin-25.0.4.7--alpine3.24-blue.svg)]([https://opensource.org/licenses/MIT](https://hub.docker.com/hardened-images/catalog/dhi/eclipse-temurin/images/eclipse-temurin%2Falpine-3.24%2Fjre-25/sha256-bd7ce9b10bc0bb04e7d1f55388d6b8e19e38f3497491425132027f6af85e47b0))

> _"Don't maintain configuration in five different places. Put them in Git."_

# mihaly-farkas/spring-boot-config-server

A containerized [Spring&nbsp;Cloud&nbsp;Config](https://spring.io/projects/spring-cloud-config) server.

## ⚠️ Disclaimer & Liability

This is a hobby project. I make no guarantee that it is production-ready. The project may contain experimental or incomplete features.

Use it at your own risk, and carefully review and adapt the configuration before using it in your own environment.

## 🚀 Quick Start

1. Run the container with the necessary property. For example, to consume configuration from this project's [example config repo](https://github.com/mihaly-farkas/spring-boot-config-server-example) files, you can run:

   ```bash
    docker run \
      --name    spring-boot-config-server \
      --publish 8888:8888 \
      ghcr.io/mihaly-farkas/spring-boot-config-server:unstable \
      --spring.cloud.config.server.git.uri='https://github.com/mihaly-farkas/spring-boot-config-server-example' \
      --encrypt.key='3x4mp13_r3p0_S3cur3_3Nc1pT1on_k3Y' \
      --spring.profiles.active='docker,no_auth'
   ```

   This example intentionally enables the `no_auth` profile to keep local evaluation simple.

2. Visit the Swagger UI for API exploration: http://localhost:8888/swagger-ui/index.html?urls.primaryName=Spring+Cloud+Config

## 📦 Features

- **Containerized Distribution:** Distributed as a Docker image with the required runtime configuration already assembled.

  - **Hardened Runtime Image:** Built on a security-hardened, minimal Eclipse Temurin [runtime image](https://hub.docker.com/hardened-images/catalog/dhi/eclipse-temurin/images?search=25-alpine&variants=runtime) designed to minimize the runtime attack surface, with no shell or unnecessary runtime tooling included.

- **Ready-to-Use Configuration Setup:** Comes with the essential Spring Boot, Spring Cloud Config, security, encryption, and operational configuration already set up.

  - **Built-in Security:** Provides setup support for Spring Security.

  - **Health and Monitoring:** Provides Spring Boot Actuator endpoints.

  - **OpenAPI Documentation:** Provides API documentation for the exposed configuration and management endpoints.

## ⚖️ License

This project is licensed under the [MIT License](https://github.com/mihaly-farkas/spring-cloud-config-server?tab=MIT-1-ov-file).
