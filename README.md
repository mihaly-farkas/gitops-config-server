[![Continuous Delivery](https://github.com/mihaly-farkas/gitops-config-server/actions/workflows/continuous-delivery.yaml/badge.svg)](https://github.com/mihaly-farkas/gitops-config-server/actions/workflows/continuous-delivery.yaml)
[![CodeQL](https://github.com/mihaly-farkas/gitops-config-server/actions/workflows/github-code-scanning/codeql/badge.svg)](https://github.com/mihaly-farkas/gitops-config-server/actions/workflows/github-code-scanning/codeql)
[![Dependabot Updates](https://github.com/mihaly-farkas/gitops-config-server/actions/workflows/dependabot/dependabot-updates/badge.svg)](https://github.com/mihaly-farkas/gitops-config-server/actions/workflows/dependabot/dependabot-updates)

[![Sonar quality gate status](https://sonarcloud.io/api/project_badges/measure?project=mihaly-farkas_gitops-config-server&metric=alert_status&token=d45a54717c09a5b9b40a9a8d07214aad8d92c9dc)](https://sonarcloud.io/summary/new_code?id=mihaly-farkas_gitops-config-server)
[![Sonar coverage](https://sonarcloud.io/api/project_badges/measure?project=mihaly-farkas_gitops-config-server&metric=coverage&token=d45a54717c09a5b9b40a9a8d07214aad8d92c9dc)](https://sonarcloud.io/summary/new_code?id=mihaly-farkas_gitops-config-server)
[![Sonar Security Rating](https://sonarcloud.io/api/project_badges/measure?project=mihaly-farkas_gitops-config-server&metric=security_rating&token=d45a54717c09a5b9b40a9a8d07214aad8d92c9dc)](https://sonarcloud.io/summary/new_code?id=mihaly-farkas_gitops-config-server)
[![Sonar Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=mihaly-farkas_gitops-config-server&metric=sqale_index&token=d45a54717c09a5b9b40a9a8d07214aad8d92c9dc)](https://sonarcloud.io/summary/new_code?id=mihaly-farkas_gitops-config-server)

[![Java 25 LTS](https://img.shields.io/badge/java-25%20LTS-blue.svg)](https://www.oracle.com/java/technologies/javase/jdk25-archive-downloads.html)
[![Maven 3.9.16](https://img.shields.io/badge/maven-3.9.16-blue.svg)](https://maven.apache.org/download.cgi)
[![Spring Boot 4.1.0](https://img.shields.io/badge/spring%20boot-4.1.0-blue.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud 2025.1.2](https://img.shields.io/badge/spring%20cloud-2025.1.2-blue.svg)](https://spring.io/projects/spring-cloud)
[![Dockerfile 1.26](https://img.shields.io/badge/dockerfile-1.26-blue.svg)](https://docs.docker.com/engine/reference/builder/)
[![dhi.io/eclipse-temurin](https://img.shields.io/badge/dhi.io%2Feclipse--temurin-25.0.4.7--alpine3.24-blue.svg)]([https://opensource.org/licenses/MIT](https://hub.docker.com/hardened-images/catalog/dhi/eclipse-temurin/images/eclipse-temurin%2Falpine-3.24%2Fjre-25/sha256-bd7ce9b10bc0bb04e7d1f55388d6b8e19e38f3497491425132027f6af85e47b0))



> _"Don't maintain configuration in five different places. Put them in Git."_

# GitOps Config Server

A containerized configuration service for GitOps-style configuration management.

## ⚠️ Disclaimer & Liability

This is one of my hobby projects. While I strive to follow professional standards and best practices, I make no guarantee that it is production-ready. The project may contain experimental or incomplete features.

Use it at your own risk, and carefully review and adapt the configuration before using it in your own environment.

## 💡 Motivation

_Configuration management_ is an often-overlooked part of the software delivery process, despite being just as important as the code itself.

In the spirit of _Infrastructure as Code_ and the broader _Everything-as-Code_ philosophy, I want to apply the same principles to configuration management.

The goal is not to build yet another centralized configuration platform. The goal is to make configuration delivery easy and Git-driven.

Instead of maintaining environment properties across different platforms, manual dashboards, environment-specific copies, file systems, environment variables, and scattered configuration files, I want configuration to be:

### 📦 One Source of Truth

The configuration should be stored in a single place. I want to manage configuration for applications, deployment pipelines, and infrastructure provisioning from the same source and using consistent configuration models.

### 🛠️ Convenient to Use

I want to store all configuration in a single GitHub repository so that it fits naturally into the rest of the project environment and CI/CD pipeline.

Configuration should be manageable directly through the GitHub web interface or from my favorite IDE, just like any other code.

The solution should support variable substitution and templating to avoid duplicated configuration values, copying derived values around, and introducing copy-paste errors. The same configuration should also be consumable by different applications, environments, and deployment pipelines in the formats they need.

Leveraging full power of Git and GitHub, many other benefits can be applied to configuration management, including versioning, branching, reviewability, reusability, and automation.

### 🔐 Secure by Design

I want to store sensitive configuration values in encrypted form, ensuring that they can only be decrypted by authorized users or systems that have access to the appropriate decryption key. This approach allows me to leverage GitHub's security features while adding a layer of protection for sensitive data.

### 🪶 Lightweight, Reusable, Disposable and Reproducible

I want configuration to be reusable and reproducible whenever and wherever it is needed, across applications and environments, including CI/CD pipelines, deployment scripts, and infrastructure provisioning. The solution should be simple and lightweight enough to be spun up quickly for any project, application stack, environment, or CI/CD pipeline, and disposable when it is no longer needed.

## 🚀 Quick Start

1. Run the container with the necessary property. For example, to consume configuration from this project's [example config repo](https://github.com/mihaly-farkas/gitops-config-server-example) files, you can run:

   ```bash
    docker run \
      --name    gitops-config-server \
      --publish 8888:8888 \
      ghcr.io/mihaly-farkas/gitops-config-server:unstable \
      --spring.cloud.config.server.git.uri='https://github.com/mihaly-farkas/gitops-config-server-example' \
      --encrypt.key='3x4mp13_r3p0_S3cur3_3Nc1pT1on_k3Y' \
      --spring.profiles.active='docker,no_auth'
   ```

2. Visit the Swagger UI for API exploration: http://localhost:8888/swagger-ui/index.html?urls.primaryName=Spring+Cloud+Config

## 📦 Features

- **Spring Cloud Config:** Built on top of [Spring&nbsp;Cloud&nbsp;Config&nbsp;Server](https://spring.io/projects/spring-cloud-config), it provides compatibility with the Spring Cloud Config ecosystem, including application- and profile-based configuration, labels, placeholder resolution, configuration overrides, and other standard Spring Cloud Config features.

- **Containerized Distribution:** Distributed as a Docker image with the required runtime configuration already assembled.

  - **Hardened Runtime Image:** Built on a security-hardened, minimal Eclipse Temurin [runtime image](https://hub.docker.com/hardened-images/catalog/dhi/eclipse-temurin/images?search=25-alpine&variants=runtime) designed to minimize the runtime attack surface, with no shell or unnecessary runtime tooling included.

  - **Native Docker Health Check:** Includes a built-in [Docker health check](https://docs.docker.com/reference/dockerfile/#healthcheck) that verifies the application's actual health and exposes it through Docker's native health status, allowing orchestration tools such as Docker Compose to monitor the application without additional health check configuration.

- **Ready-to-Use Configuration Setup:** Comes with the essential Spring Boot, Spring Cloud Config, security, encryption, and operational configuration already set up.

  - **Built-in Security:** Provides setup support for Spring Security.

  - **Health and Monitoring:** Provides Spring Boot Actuator endpoints.

  - **OpenAPI Documentation:** Provides API documentation for the exposed configuration and management endpoints.

## 🏗️ Architecture

How you structure your own solution is entirely up to you. The diagram below illustrates a few possible setups, but the config server can be integrated into any architecture.

```mermaid
flowchart LR
  style pipeline stroke-dasharray: 5 5
  style pipelineProcess stroke-dasharray: 5 5
  style pipelineConfigServer stroke-dasharray: 5 5
  style configRepositoryCdWorkflowIac stroke-dasharray: 5 5
  style configRepositoryCdWorkflowApp stroke-dasharray: 5 5
  style iacRepositoryCdWorkflow stroke-dasharray: 5 5
  style appRepositoryCdWorkflow stroke-dasharray: 5 5
  devopsEngineer(["👤\nDevOps Engineer\n---\nManages configuration in GitHub for everything"]):::person

  subgraph github["GitHub"]
    subgraph configRepository["Config Repository"]
      configRepositoryGit["«private»\nGit Repository\n---\nStores plain-text and encrypted configuration values"]:::container
      configRepositoryCdWorkflowIac["CD Workflow"]
      configRepositoryCdWorkflowApp["CD Workflow"]
    end
    subgraph iacRepository["IaC Repository"]
      iacRepositoryGit["«private»\nGit Repository\n---\nStores Infrastructure as Code (IaC) scripts and templates"]:::container
      iacRepositoryCdWorkflow["CD Workflow"]
    end
    subgraph appRepository["App Repository"]
      appRepositoryGit["«private»\nGit Repository\n---\nStores application code"]:::container
      appRepositoryCdWorkflow["CD Workflow"]
    end
  end

  subgraph stack1["Stack"]
    stack1ConfigServer["GitOps Config Server"]:::container
    stack1Client1["Client Application"]:::container
    stack1Client2["Client Application"]:::container
    stack1Client3["Client Application"]:::container
  end

  subgraph stack2["Stack"]
    stack2Client1["Spring Boot Microservice\n---\nWith embedded GitOps Config Server"]:::container
    stack2Client2["Spring Boot Microservice\n---\nWith embedded GitOps Config Server"]:::container
  end

  subgraph pipeline["Pipeline"]
    pipelineConfigServer["«temp»\nGitOps Config Server"]:::container
    pipelineProcess["Process"]:::container
  end

  cloudProvider["Cloud Provider"]:::external
  devopsEngineer -->|" manages configuration using\n[command line] "| configRepositoryGit
  devopsEngineer -->|" manages configuration using\n[web ui] "| configRepositoryGit
  stack1ConfigServer -->|" retrieves configuration from "| configRepositoryGit
  stack1Client1 -->|" retrieves configuration from "| stack1ConfigServer
  stack1Client2 -->|" retrieves configuration from "| stack1ConfigServer
  stack1Client3 -->|" retrieves configuration from "| stack1ConfigServer
  stack2Client1 -->|" retrieves configuration from "| configRepositoryGit
  stack2Client2 -->|" retrieves configuration from "| configRepositoryGit
  pipelineConfigServer .->|" retrieves configuration from "| configRepositoryGit
  pipelineProcess .->|" retrieves configuration from "| pipelineConfigServer
  iacRepositoryGit .->|" triggers on IaC code change "| iacRepositoryCdWorkflow
  iacRepositoryCdWorkflow .->|" retrieves configuration from "| configRepositoryGit
  iacRepositoryCdWorkflow .->|" updates infrastructure in "| cloudProvider
  appRepositoryGit .->|" triggers on application code change "| appRepositoryCdWorkflow
  appRepositoryCdWorkflow .->|" retrieves configuration from "| configRepositoryGit
  appRepositoryCdWorkflow .->|" deploys application to "| cloudProvider
  configRepositoryGit .->|" triggers on IaC configuration change "| configRepositoryCdWorkflowIac
  configRepositoryCdWorkflowIac ..->|" reuse workflow from "| iacRepositoryCdWorkflow
  configRepositoryGit .->|" triggers on application configuration change "| configRepositoryCdWorkflowApp
  configRepositoryCdWorkflowApp ..->|" reuse workflow from "| appRepositoryCdWorkflow
```

## ⚖️ License

This project is licensed under the [MIT License](https://github.com/mihaly-farkas/gitops-config-server?tab=MIT-1-ov-file).
