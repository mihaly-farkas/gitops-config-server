#!/usr/bin/env bash

#######################################################################################################################
# Build, verify and publish the Maven artifact
#######################################################################################################################

# Set strict mode for bash
set -euo pipefail

# Load ANSI color escape sequences
source .github/workflows/utils/ansi-colors.sh

# If the artifact should be published, ...
if [[ "${PUBLISH_MAVEN_ARTIFACT}" == "true" ]]; then

  # Build, verify and publish the Maven artifact
  echo "::group::${MESSAGE_COLOR}Build and publish Maven artifact${RESET_COLORS}"
  set -x
  ./mvnw --batch-mode \
    spotless:check \
    verify \
    sonar:sonar \
    deploy
  { set +x; } 2>/dev/null
  echo "::endgroup::"

  # Write a summary of the Maven build to the GitHub Actions summary
  {
    echo "## Maven Build summary"
    echo ""
    echo "The Maven build has completed successfully for the **${MAVEN_ARTIFACT_GROUP_ID}:${MAVEN_ARTIFACT_ID}:${MAVEN_ARTIFACT_VERSION}** artifact and it has been published to the [GitHub Packages Maven repository](https://github.com/${GITHUB_REPOSITORY}/packages)."
    echo ""
    echo "Checks run during the Maven build:"
    echo "- \`spotless:check\` - checks that the source code is properly formatted"
    echo "- \`verify\` - runs unit tests and integration tests"
    echo "- \`sonar:sonar\` - runs SonarQube analysis"
    echo ""
  } >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
else

  # Get the Maven artifact from the  Maven repository
  echo "::group::${MESSAGE_COLOR}Download Maven artifact${RESET_COLORS}"
  set -x
    ./mvnw --batch-mode \
      dependency:get \
      "-Dartifact=${MAVEN_ARTIFACT_GROUP_ID}:${MAVEN_ARTIFACT_ID}:${MAVEN_ARTIFACT_VERSION}"
  { set +x; } 2>/dev/null
  echo "::endgroup::"

  # Copy the Maven artifact to the target directory
  mkdir -p target
  cp "${HOME}/.m2/repository/${MAVEN_ARTIFACT_GROUP_ID//.//}/${MAVEN_ARTIFACT_ID}/${MAVEN_ARTIFACT_VERSION}/${MAVEN_ARTIFACT_ID}-${MAVEN_ARTIFACT_VERSION}.jar" target/

  # Write a summary of the artifact retrieval to the GitHub Actions summary
  {
    echo "## Maven Build summary"
    echo ""
    echo "The Maven artifact **${MAVEN_ARTIFACT_GROUP_ID}:${MAVEN_ARTIFACT_ID}:${MAVEN_ARTIFACT_VERSION}** is already published to the [GitHub Packages Maven repository](https://github.com/${GITHUB_REPOSITORY}/packages)."
    echo ""
  } >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
fi
