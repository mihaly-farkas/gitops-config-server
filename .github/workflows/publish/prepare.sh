#!/usr/bin/env bash

# Ensure the Maven wrapper is executable
chmod +x ./mvnw

# Load ANSI color escape sequences
source .github/workflows/utils/ansi-colors.sh

# Get the artifact info
MAVEN_ARTIFACT_GROUP_ID=$(./mvnw help:evaluate -Dexpression=project.groupId --quiet -DforceStdout)
MAVEN_ARTIFACT_ID=$(./mvnw help:evaluate -Dexpression=project.artifactId --quiet -DforceStdout)
MAVEN_ARTIFACT_VERSION=$(./mvnw help:evaluate -Dexpression=project.version --quiet -DforceStdout)

# Get the GitHub repository name
GITHUB_REPOSITORY_NAME=$(basename "${GITHUB_REPOSITORY}")

# Check if the Maven artifact id is the same as the GitHub repository name
if [[ "${MAVEN_ARTIFACT_ID}" != "${GITHUB_REPOSITORY_NAME}" ]]; then
  echo "::error file=.github/workflows/publish/prepare.sh,line=14,title=Maven Artifact ID Mismatch::The Maven artifact id (${VALUE_COLOR}${MAVEN_ARTIFACT_ID}${RESET_COLORS}) does not match the GitHub repository name (${ERROR_COLOR}${GITHUB_REPOSITORY_NAME}${RESET_COLORS})."
  exit 1
fi

# Determine if the version is a SNAPSHOT
if [[ "${MAVEN_ARTIFACT_VERSION}" == *"-SNAPSHOT" ]]; then
  MAVEN_IS_SNAPSHOT=true
else
  MAVEN_IS_SNAPSHOT=false
fi

# Initialize variables
PUBLISH_MAVEN_ARTIFACT="true"
PUBLISH_DOCKER_IMAGE="true"

# If the version is not a SNAPSHOT, check if the Maven artifact and Docker image are already published
if [[ "${MAVEN_IS_SNAPSHOT}" == "false" ]]; then

  # Check if the Maven artifact is already published
  PUBLISH_MAVEN_ARTIFACT=$(
    ./mvnw dependency:get \
      "-Dartifact=${MAVEN_ARTIFACT_GROUP_ID}:${MAVEN_ARTIFACT_ID}:${MAVEN_ARTIFACT_VERSION}" \
      --quiet 2>/dev/null \
      && echo "false" \
      || echo "true"
  )

  # Check if the Docker image is already published
  PUBLISH_DOCKER_IMAGE=$(
    docker pull "ghcr.io/${GITHUB_REPOSITORY}:${MAVEN_ARTIFACT_VERSION}" >/dev/null 2>&1 \
      && echo "false" \
      || echo "true"
  )
fi

# Output the artifact info
echo "${MESSAGE_COLOR}Maven artifact Group ID: ${VALUE_COLOR}${MAVEN_ARTIFACT_GROUP_ID}${RESET_COLORS}"
echo "${MESSAGE_COLOR}Maven artifact ID:       ${VALUE_COLOR}${MAVEN_ARTIFACT_ID}${RESET_COLORS}"
echo "${MESSAGE_COLOR}Maven artifact Version:  ${VALUE_COLOR}${MAVEN_ARTIFACT_VERSION}${RESET_COLORS}"
echo "${MESSAGE_COLOR}Maven is SNAPSHOT:       ${VALUE_COLOR}${MAVEN_IS_SNAPSHOT}${RESET_COLORS}"
echo "${MESSAGE_COLOR}Publish Maven artifact:  ${VALUE_COLOR}${PUBLISH_MAVEN_ARTIFACT}${RESET_COLORS}"
echo "${MESSAGE_COLOR}Publish Docker image:    ${VALUE_COLOR}${PUBLISH_DOCKER_IMAGE}${RESET_COLORS}"

# Output variables for use in subsequent steps
# shellcheck disable=SC2129
echo "maven-artifact-group-id=${MAVEN_ARTIFACT_GROUP_ID}" >> "${GITHUB_OUTPUT}"
echo "maven-artifact-id=${MAVEN_ARTIFACT_ID}"             >> "${GITHUB_OUTPUT}"
echo "maven-artifact-version=${MAVEN_ARTIFACT_VERSION}"   >> "${GITHUB_OUTPUT}"
echo "maven-is-snapshot=${MAVEN_IS_SNAPSHOT}"             >> "${GITHUB_OUTPUT}"
echo "publish-maven-artifact=${PUBLISH_MAVEN_ARTIFACT}"   >> "${GITHUB_OUTPUT}"
echo "publish-docker-image=${PUBLISH_DOCKER_IMAGE}"       >> "${GITHUB_OUTPUT}"
