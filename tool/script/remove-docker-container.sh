#!/usr/bin/env bash

#######################################################################################################################
# Remove Docker container
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

# If spring-cloud-config-server container is already running, stop it
echo "${MESSAGE_COLOR}---------------------------------------------------------------------------------${RESET_COLORS}"
echo "${MESSAGE_COLOR}Checking if spring-cloud-config-server container is running...${RESET_COLORS}"
echo "${MESSAGE_COLOR}---------------------------------------------------------------------------------${RESET_COLORS}"
process_status=$({
  set -x
  docker ps --filter "name=spring-cloud-config-server" --format '{{.Names}}'
  { set +x; } 2>/dev/null
})
if echo "${process_status}" | grep -q '^spring-cloud-config-server$'; then
  echo "${MESSAGE_COLOR}Stopping existing spring-cloud-config-server container...${RESET_COLORS}"
  set -x
  docker stop spring-cloud-config-server
  { set +x; } 2>/dev/null
  echo "${OK_COLOR}Done.${RESET_COLORS}"
else
  echo "${MESSAGE_COLOR}No running spring-cloud-config-server container found.${RESET_COLORS}"
fi

# If spring-cloud-config-server container exists, remove it
process_status=$({
  set -x
  docker ps --all --filter "name=spring-cloud-config-server" --format '{{.Names}}'
  { set +x; } 2>/dev/null
})
if echo "${process_status}" | grep -q '^spring-cloud-config-server$'; then
  echo "${MESSAGE_COLOR}Removing existing spring-cloud-config-server container...${RESET_COLORS}"
  set -x
  docker rm spring-cloud-config-server
  { set +x; } 2>/dev/null
  echo "${OK_COLOR}Done.${RESET_COLORS}"
else
  echo "${MESSAGE_COLOR}No existing spring-cloud-config-server container found.${RESET_COLORS}"
fi
