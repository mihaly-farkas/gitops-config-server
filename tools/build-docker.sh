#!/usr/bin/env bash

#######################################################################################################################
# Build Docker image
#######################################################################################################################

# Set strict mode for bash
set -euo pipefail

# Get the absolute path of the script directory and project root
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_dir="$(cd "${script_dir}/.." && pwd)"

# Change the working directory to the project root
cd "${project_dir}" || exit 1

# Load ANSI color escape sequences
source .github/workflows/utils/ansi-colors.sh

#######################################################################################################################

# Get Maven artifact version from the pom.xml
echo "${MESSAGE_COLOR}---------------------------------------------------------------------------------${RESET_COLORS}"
echo "${MESSAGE_COLOR}Getting Maven artifact version from pom.xml...${RESET_COLORS}"
echo "${MESSAGE_COLOR}---------------------------------------------------------------------------------${RESET_COLORS}"
maven_artifact_version=$({
   set -x
  ./mvnw help:evaluate -Dexpression=project.version --quiet -DforceStdout
  { set +x; } 2>/dev/null
})

# Build Docker image
echo "${MESSAGE_COLOR}---------------------------------------------------------------------------------${RESET_COLORS}"
echo "${MESSAGE_COLOR}Building Docker image...${RESET_COLORS}"
echo "${MESSAGE_COLOR}---------------------------------------------------------------------------------${RESET_COLORS}"
set -x
docker build --tag ghcr.io/mihaly-farkas/gitops-config-server:feature-test . \
  --build-arg APP_VERSION="${maven_artifact_version}" \
  --build-arg BUILD_ID="local:$(date +%s)"
{ set +x; } 2>/dev/null
