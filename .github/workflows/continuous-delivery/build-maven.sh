#!/usr/bin/env bash

#######################################################################################################################
# Verify Maven artifact
#######################################################################################################################

# Set strict mode for bash
set -euo pipefail

# Load ANSI color escape sequences
source .github/workflows/utils/ansi-colors.sh

#######################################################################################################################

# Ensure that the Maven wrapper is executable
chmod +x ./mvnw

# If the artifact should be built, ...
if [[ "${BUILD_ARTIFACT}" == "true" ]]; then

  # Run Spotless, verify and SonarQube analysis on the Maven artifact
  echo "::group::${MESSAGE_COLOR}Building and verifying Maven artifact...${RESET_COLORS}"
  set -x
  ./mvnw --batch-mode \
     spotless:check \
     verify \
     sonar:sonar
  { set +x; } 2>/dev/null
  echo "::endgroup::"

else

  # Get the Maven artifact from the  Maven repository
  echo "::group::${MESSAGE_COLOR}Downloading Maven artifact...${RESET_COLORS}"
  set -x
  ./mvnw --batch-mode \
    dependency:get \
    "-Dartifact=${MAVEN_ARTIFACT_GROUP_ID}:${MAVEN_ARTIFACT_ID}:${MAVEN_ARTIFACT_VERSION}"
  { set +x; } 2>/dev/null
  echo "::endgroup::"

  # Copy the Maven artifact to the target directory
  mkdir -p target
  cp "${HOME}/.m2/repository/${MAVEN_ARTIFACT_GROUP_ID//.//}/${MAVEN_ARTIFACT_ID}/${MAVEN_ARTIFACT_VERSION}/${MAVEN_ARTIFACT_ID}-${MAVEN_ARTIFACT_VERSION}.jar" target/
fi

