#!/usr/bin/env bash

#######################################################################################################################
# Verify features
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

# Change the working directory to the feature-runner directory
cd tools/feature-runner || exit 1

# Install dependencies
echo "${MESSAGE_COLOR}---------------------------------------------------------------------------------${RESET_COLORS}"
echo "${MESSAGE_COLOR}Installing dependencies...${RESET_COLORS}"
echo "${MESSAGE_COLOR}---------------------------------------------------------------------------------${RESET_COLORS}"
set -x
npm install --ignore-scripts
{ set +x; } 2>/dev/null

# Run fix script
echo "${MESSAGE_COLOR}---------------------------------------------------------------------------------${RESET_COLORS}"
echo "${MESSAGE_COLOR}Running fix script...${RESET_COLORS}"
echo "${MESSAGE_COLOR}---------------------------------------------------------------------------------${RESET_COLORS}"
set -x
npm run fix
{ set +x; } 2>/dev/null

# Run feature tests
echo "${MESSAGE_COLOR}---------------------------------------------------------------------------------${RESET_COLORS}"
echo "${MESSAGE_COLOR}Running feature tests...${RESET_COLORS}"
echo "${MESSAGE_COLOR}---------------------------------------------------------------------------------${RESET_COLORS}"
set -x
npm test
{ set +x; } 2>/dev/null
