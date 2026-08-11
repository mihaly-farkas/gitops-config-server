#!/usr/bin/env bash

#######################################################################################################################
# Run feature tests
#######################################################################################################################

# Set strict mode for bash
set -euo pipefail

# Load ANSI color escape sequences
source .github/workflows/utils/ansi-colors.sh

# Change the working directory to the feature-runner tool
cd tools/feature-runner || exit 1

# Run npm ci to install dependencies without modifying package-lock.json
echo "::group::${MESSAGE_COLOR}Install Node.js dependencies${RESET_COLORS}"
set -x
npm ci --ignore-scripts
{ set +x; } 2>/dev/null
echo "::endgroup::"

# Get the Docker image name and tag
echo "::group::${MESSAGE_COLOR}Run feature tests${RESET_COLORS}"
set -x
npm test
{ set +x; } 2>/dev/null
echo "::endgroup::"
