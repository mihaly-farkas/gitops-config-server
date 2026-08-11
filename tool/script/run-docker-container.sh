#!/usr/bin/env bash

#######################################################################################################################
# Run Docker container
#######################################################################################################################

# Set strict mode for bash
set -euo pipefail

# Get the absolute path of the script directory and project root
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_dir="$(cd "${script_dir}/../.." && pwd)"

# Change the working directory to the project root
cd "${project_dir}" || exit 1

# Load ANSI color escape sequences
source .github/workflows/utils/ansi-colors.sh

#######################################################################################################################

# Run the Docker container
echo "${MESSAGE_COLOR}---------------------------------------------------------------------------------${RESET_COLORS}"
echo "${MESSAGE_COLOR}Running gitops-config-server container...${RESET_COLORS}"
echo "${MESSAGE_COLOR}---------------------------------------------------------------------------------${RESET_COLORS}"
set -x
docker run \
  --name gitops-config-server \
  --publish 8888:8888 \
  ghcr.io/mihaly-farkas/gitops-config-server:local \
  --spring.cloud.config.server.git.uri='https://github.com/mihaly-farkas/gitops-config-server-example' \
  --spring.cloud.config.server.git.default-label='main' \
  --encrypt.key='3x4mp13_r3p0_S3cur3_3Nc1pT1on_k3Y'
{ set +x; } 2>/dev/null
