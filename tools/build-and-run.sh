#!/usr/bin/env bash

# Set strict mode for bash
set -euo pipefail

# Get the absolute path of the script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Change the working directory to the project root
cd "${PROJECT_DIR}" || exit 1

# If gitops-config-server container is already running, stop it
if docker ps --filter "name=gitops-config-server" --format '{{.Names}}' | grep -q '^gitops-config-server$'; then
  echo "Stopping existing gitops-config-server container..."
  docker stop gitops-config-server
fi

# If gitops-config-server container exists, remove it
if docker ps -a --filter "name=gitops-config-server" --format '{{.Names}}' | grep -q '^gitops-config-server$'; then
  echo "Removing existing gitops-config-server container..."
  docker rm gitops-config-server
fi

# Build the Docker image
source "${SCRIPT_DIR}/build.sh"

# Run the Docker container
docker run \
  --name    gitops-config-server \
  --env     CONFIG_GIT_URI=https://github.com/mihaly-farkas/gitops-config-server-example \
  --env     CONFIG_ADMIN_PASSWORD="y0uR_S3cur3_aDm1N_P4ssw0rd" \
  --env     CONFIG_ENCRYPTION_KEY="3x4mp13_r3p0_S3cur3_3Nc1pT1on_k3Y" \
  --env     LOGGING_LEVEL_ROOT="INFO" \
  --env     LOGGING_LEVEL_CONFIG_DEBUG_LISTENER="DEBUG" \
  --publish 8888:8888 \
  ghcr.io/mihaly-farkas/gitops-config-server:f0ature-test
