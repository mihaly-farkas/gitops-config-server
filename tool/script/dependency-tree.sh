#!/usr/bin/env bash

#######################################################################################################################
# Build project
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

# Build with Maven
echo "${MESSAGE_COLOR}---------------------------------------------------------------------------------${RESET_COLORS}"
echo "${MESSAGE_COLOR}Generating Maven dependency tree...${RESET_COLORS}"
echo "${MESSAGE_COLOR}---------------------------------------------------------------------------------${RESET_COLORS}"
set -x
./mvnw dependency:tree -Dscope=compile > dependency-tree.txt
{ set +x; } 2>/dev/null

set -x
./mvnw dependency:tree -Dscope=compile -P !security_override > dependency-tree-without-security-overrides.txt
{ set +x; } 2>/dev/null
