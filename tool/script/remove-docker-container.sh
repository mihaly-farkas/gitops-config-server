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

# If gitops-config-server container is already running, stop it
echo "${MESSAGE_COLOR}---------------------------------------------------------------------------------${RESET_COLORS}"
echo "${MESSAGE_COLOR}Checking if gitops-config-server container is running...${RESET_COLORS}"
echo "${MESSAGE_COLOR}---------------------------------------------------------------------------------${RESET_COLORS}"
process_status=$({
  set -x
  docker ps --filter "name=gitops-config-server" --format '{{.Names}}'
  { set +x; } 2>/dev/null
})
if echo "${process_status}" | grep -q '^gitops-config-server$'; then
  echo "${MESSAGE_COLOR}Stopping existing gitops-config-server container...${RESET_COLORS}"
  set -x
  docker stop gitops-config-server
  { set +x; } 2>/dev/null
  echo "${OK_COLOR}Done.${RESET_COLORS}"
else
  echo "${MESSAGE_COLOR}No running gitops-config-server container found.${RESET_COLORS}"
fi

# If gitops-config-server container exists, remove it
process_status=$({
  set -x
  docker ps --all --filter "name=gitops-config-server" --format '{{.Names}}'
  { set +x; } 2>/dev/null
})
if echo "${process_status}" | grep -q '^gitops-config-server$'; then
  echo "${MESSAGE_COLOR}Removing existing gitops-config-server container...${RESET_COLORS}"
  set -x
  docker rm gitops-config-server
  { set +x; } 2>/dev/null
  echo "${OK_COLOR}Done.${RESET_COLORS}"
else
  echo "${MESSAGE_COLOR}No existing gitops-config-server container found.${RESET_COLORS}"
fi
