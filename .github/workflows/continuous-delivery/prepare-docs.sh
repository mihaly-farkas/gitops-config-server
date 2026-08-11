#!/usr/bin/env bash

#######################################################################################################################
# Prepare Documentation for GitHub Pages
#######################################################################################################################

# Set strict mode for bash
set -euo pipefail

# Load ANSI color escape sequences
source .github/workflows/utils/ansi-colors.sh

rm -rf .tmp/github-pages
mkdir -p .tmp/github-pages

echo "::group::${MESSAGE_COLOR}Collect feature test results${RESET_COLORS}"

# Iterate over the ${DOCKER_TAGS}
for TAG in ${DOCKER_TAGS}; do
  echo "Processing Docker tag: ${TAG}"
  mkdir -p ".tmp/github-pages/docker/images/${TAG}/features"
  cp -r tools/feature-runner/reports/cucumber-report.html ".tmp/github-pages/docker/images/${TAG}/features/index.html"
  # Replace "ghcr.io/${GITHUB_REPOSITORY}:feature-test" string in the index.html to "ghcr.io/${GITHUB_REPOSITORY}:${TAG}"
  sed -i "s|ghcr.io/${GITHUB_REPOSITORY}:feature-test|ghcr.io/${GITHUB_REPOSITORY}:${TAG}|g" ".tmp/github-pages/docker/images/${TAG}/features/index.html"
done

echo "::endgroup::"

find .tmp/github-pages
