#!/usr/bin/env bash

#######################################################################################################################
# Run feature tests
#######################################################################################################################

# Set strict mode for bash
set -euo pipefail

# Load ANSI color escape sequences
source .github/workflows/utils/ansi-colors.sh

#######################################################################################################################

# Change the working directory to the feature-runner tool
cd tool/feature-runner || exit 1

# Run npm ci to install dependencies without modifying package-lock.json
echo "::group::${MESSAGE_COLOR}Installing Node.js dependencies...${RESET_COLORS}"
set -x
npm ci --ignore-scripts
{ set +x; } 2>/dev/null
echo "::endgroup::"

# Get the Docker image name and tag
echo "::group::${MESSAGE_COLOR}Running feature tests...${RESET_COLORS}"
set -x
npm test
{ set +x; } 2>/dev/null
echo "::endgroup::"

# TODO improve the Cucumber test reports and add them to the GitHub Actions summary

# Write a GitHub Actions summary
{
  echo "## Feature Tests summary"
  echo ""
  echo "The feature tests have completed successfully."
} >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
