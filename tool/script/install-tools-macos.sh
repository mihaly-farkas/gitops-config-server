#!/usr/bin/env bash

# Set strict mode for bash
set -euo pipefail

# Get the absolute path of the script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

# Change the working directory to the project root
cd "${PROJECT_DIR}" || exit 1


brew install act
brew install copilot-cli
brew install copier
