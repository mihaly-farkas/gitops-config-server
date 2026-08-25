#!/usr/bin/env bash

#######################################################################################################################
# Verify Maven artifact
#######################################################################################################################

# Set strict mode for bash
set -euo pipefail

# Load ANSI color escape sequences
source .github/workflows/utils/ansi-colors.sh

#######################################################################################################################

pom_size=$(du -k pom.xml | cut -f1)
main_jar_size=$(du -k "target/${MAVEN_ARTIFACT_ID}-${MAVEN_ARTIFACT_VERSION}.jar" | cut -f1)

# If the artifact should be built, ...
if [[ "${BUILD_ARTIFACT}" == "true" ]]; then

  # Write a GitHub Actions summary
  {
    echo "## Maven Build summary"
    echo ""
    echo "⬇️ [pom.xml](${BUILD_ARTIFACT_POM_URL}) (${pom_size} KB)"
      echo "⬇️ [${MAVEN_ARTIFACT_ID}-${MAVEN_ARTIFACT_VERSION}.jar](${BUILD_ARTIFACT_MAIN_JAR_URL}) (${main_jar_size} KB)"
    echo ""
    echo "|  Maven Goal        | Description                                                                                                                |"
    echo "|--------------------|----------------------------------------------------------------------------------------------------------------------------|"
    echo "| \`spotless:check\` | Checks that the source code is properly formatted using [google-java-format](https://github.com/google/google-java-format) |"
    echo "| \`verify\`         | Runs unit tests and integration tests                                                                                      |"
    echo "| \`sonar:sonar\`    | Runs [SonarQube](https://sonarcloud.io/summary/overall?id=mihaly-farkas_spring-cloud-config-server&branch=main) analysis         |"
  } >> "${GITHUB_STEP_SUMMARY:-/dev/null}"

else

  # Write a GitHub Actions summary
  {
    echo "## Download Maven Artifact summary"
    echo ""
    echo "Maven artifact is already published. Downloaded from [GitHub Packages Maven Repository](https://github.com/${GITHUB_REPOSITORY}/packages)."
    echo ""
    echo "⬇️ [${MAVEN_ARTIFACT_ID}-${MAVEN_ARTIFACT_VERSION}.jar](${BUILD_ARTIFACT_MAIN_JAR_URL}) (${main_jar_size} KB)"
    echo "⬇️ [pom.xml](${BUILD_ARTIFACT_POM_URL}) (${pom_size} KB)"
  } >> "${GITHUB_STEP_SUMMARY:-/dev/null}"

fi

