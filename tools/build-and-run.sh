#!/usr/bin/env bash

#######################################################################################################################
# Build project and run container
#######################################################################################################################

# Set strict mode for bash
set -euo pipefail

# Get the absolute path of the script directory and project root
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
project_dir="$(cd "${script_dir}/.." && pwd)"

# Change the working directory to the project root
cd "${project_dir}" || exit 1

#######################################################################################################################

# Remove Docker container
source tools/remove-docker-container.sh

# Build Spring Boot app
source tools/build-maven.sh

# Build Docker image
source tools/build-docker.sh

# Run Docker container
source tools/run-docker-container.sh
