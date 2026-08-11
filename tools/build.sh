#!/usr/bin/env bash

# Set strict mode for bash
set -euo pipefail

# Get the absolute path of the script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Change the working directory to the project root
cd "${PROJECT_DIR}" || exit 1

# Build the project using Maven
./mvnw --batch-mode clean package -DskipTests

# Get the Maven artifact version from the pom.xml
MAVEN_ARTIFACT_VERSION=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout)

# Build the Docker image
docker build --tag ghcr.io/mihaly-farkas/gitops-config-server:feature-test . --build-arg APP_VERSION="${MAVEN_ARTIFACT_VERSION}"
