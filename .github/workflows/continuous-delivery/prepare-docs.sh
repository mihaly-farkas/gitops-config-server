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

# Iterate over the ${DOCKER_IMAGE_REFERENCES}
for IMAGE_REFERENCE in ${DOCKER_IMAGE_REFERENCES}; do

  TAG=$(echo "${IMAGE_REFERENCE}" | cut -d':' -f2)

  echo "Processing Docker tag: ${TAG}"

  mkdir -p ".tmp/github-pages/docker/images/${TAG}/features"
  cp -r tools/feature-runner/reports/cucumber-report.html ".tmp/github-pages/docker/images/${TAG}/features/index.html"

  # Replace "ghcr.io/${GITHUB_REPOSITORY}:feature-test" string in the index.html to "ghcr.io/${GITHUB_REPOSITORY}:${DOCKER_IMAGE_REFERENCE}"
  sed -i "s|ghcr.io/${GITHUB_REPOSITORY}:feature-test|ghcr.io/${GITHUB_REPOSITORY}:${TAG}|g" ".tmp/github-pages/docker/images/${TAG}/features/index.html"
done

echo "::endgroup::"

find .tmp/github-pages
