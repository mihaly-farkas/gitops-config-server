#!/usr/bin/env bash

#######################################################################################################################
# Prepare build environment
#######################################################################################################################

# Set strict mode for bash
set -euo pipefail

# Load ANSI color escape sequences
source .github/workflows/utils/ansi-colors.sh

#######################################################################################################################

# Ensure that the Maven wrapper is executable
chmod +x ./mvnw

# Get the artifact info
MAVEN_ARTIFACT_GROUP_ID=$(./mvnw help:evaluate -Dexpression=project.groupId --quiet -DforceStdout)
MAVEN_ARTIFACT_ID=$(./mvnw help:evaluate -Dexpression=project.artifactId --quiet -DforceStdout)
MAVEN_ARTIFACT_VERSION=$(./mvnw help:evaluate -Dexpression=project.version --quiet -DforceStdout)
MAVEN_ARTIFACT_VERSION_MAJOR=$(echo "${MAVEN_ARTIFACT_VERSION}" | cut -d. -f1)
MAVEN_ARTIFACT_VERSION_MINOR=$(echo "${MAVEN_ARTIFACT_VERSION}" | cut -d. -f2)
MAVEN_ARTIFACT_VERSION_PATCH=$(echo "${MAVEN_ARTIFACT_VERSION}" | cut -d- -f1 | cut -d. -f3)

# Get the GitHub repository name
GITHUB_REPOSITORY_NAME=$(basename "${GITHUB_REPOSITORY}")

# Check if the Maven artifact id is the same as the GitHub repository name
if [[ "${MAVEN_ARTIFACT_ID}" != "${GITHUB_REPOSITORY_NAME}" ]]; then
  echo "::error file=.github/workflows/continuous-delivery/prepare.sh,line=27,title=Maven Artifact ID Mismatch::The Maven artifact id (${VALUE_COLOR}${MAVEN_ARTIFACT_ID}${RESET_COLORS}) does not match the GitHub repository name (${ERROR_COLOR}${GITHUB_REPOSITORY_NAME}${RESET_COLORS})."
  exit 1
fi

# Get the current Git commit short hash
GIT_COMMIT_SHORT_HASH=$(git rev-parse --short HEAD)

# Get the current Git commit long hash
GIT_COMMIT_LONG_HASH=$(git rev-parse HEAD)

# Get the current Git commit timestamp
GIT_COMMIT_TIMESTAMP=$(git log -1 --format=%ct)

# Determine if the version is a SNAPSHOT
if [[ "${MAVEN_ARTIFACT_VERSION}" == *"-SNAPSHOT" ]]; then
  MAVEN_IS_SNAPSHOT=true
else
  MAVEN_IS_SNAPSHOT=false
fi

# Determine if the current Git commit is on the main branch (even if it is a detached HEAD)
if git branch --contains HEAD | grep -q "main"; then
  GIT_IS_MAIN_BRANCH=true
else
  GIT_IS_MAIN_BRANCH=false
fi

# If it is on the main branch, determine if the current commit is the latest commit on the main branch
if [[ "${GIT_IS_MAIN_BRANCH}" == "true" ]]; then
  if [[ "$(git rev-parse HEAD)" == "$(git rev-parse origin/main)" ]]; then
    GIT_IS_LATEST_MAIN_BRANCH_COMMIT=true
  else
    GIT_IS_LATEST_MAIN_BRANCH_COMMIT=false
  fi
else
  GIT_IS_LATEST_MAIN_BRANCH_COMMIT=false
fi

# Initialize the Docker image tags variable
DOCKER_TAGS=""

# If:
#   - the version is a SNAPSHOT and
#   - the current commit is on the `main` branch and
#   - it is the latest commit on the `main` branch
# then:
#   - add the `unstable` tag to the Docker image tags
#   - add the `beta` tag to the Docker image tags
#   - add the `${MAVEN_ARTIFACT_VERSION_MAJOR}-beta` tag to the Docker image tags
#   - add the `${MAVEN_ARTIFACT_VERSION_MAJOR}-beta.${GIT_COMMIT_TIMESTAMP}` tag to the Docker image tags
#   - add the `${MAVEN_ARTIFACT_VERSION_MAJOR}.${MAVEN_ARTIFACT_VERSION_MINOR}-beta` tag to the Docker image tags
#   - add the `${MAVEN_ARTIFACT_VERSION_MAJOR}.${MAVEN_ARTIFACT_VERSION_MINOR}-beta.${GIT_COMMIT_TIMESTAMP}` tag to the Docker image tags
#   - add the `${MAVEN_ARTIFACT_VERSION_MAJOR}.${MAVEN_ARTIFACT_VERSION_MINOR}.${MAVEN_ARTIFACT_VERSION_PATCH}-beta` tag to the Docker image tags
#   - add the `${MAVEN_ARTIFACT_VERSION_MAJOR}.${MAVEN_ARTIFACT_VERSION_MINOR}.${MAVEN_ARTIFACT_VERSION_PATCH}-beta.${GIT_COMMIT_TIMESTAMP}` tag to the Docker image tags
if [[ "${MAVEN_IS_SNAPSHOT}" == "true" && "${GIT_IS_MAIN_BRANCH}" == "true" && "${GIT_IS_LATEST_MAIN_BRANCH_COMMIT}" == "true" ]]; then
  DOCKER_TAGS="${DOCKER_TAGS} unstable"
  DOCKER_TAGS="${DOCKER_TAGS} beta"
  DOCKER_TAGS="${DOCKER_TAGS} ${MAVEN_ARTIFACT_VERSION_MAJOR}-beta"
  DOCKER_TAGS="${DOCKER_TAGS} ${MAVEN_ARTIFACT_VERSION_MAJOR}-beta.${GIT_COMMIT_TIMESTAMP}"
  DOCKER_TAGS="${DOCKER_TAGS} ${MAVEN_ARTIFACT_VERSION_MAJOR}.${MAVEN_ARTIFACT_VERSION_MINOR}-beta"
  DOCKER_TAGS="${DOCKER_TAGS} ${MAVEN_ARTIFACT_VERSION_MAJOR}.${MAVEN_ARTIFACT_VERSION_MINOR}-beta.${GIT_COMMIT_TIMESTAMP}"
  DOCKER_TAGS="${DOCKER_TAGS} ${MAVEN_ARTIFACT_VERSION_MAJOR}.${MAVEN_ARTIFACT_VERSION_MINOR}.${MAVEN_ARTIFACT_VERSION_PATCH}-beta"
  DOCKER_TAGS="${DOCKER_TAGS} ${MAVEN_ARTIFACT_VERSION_MAJOR}.${MAVEN_ARTIFACT_VERSION_MINOR}.${MAVEN_ARTIFACT_VERSION_PATCH}-beta.${GIT_COMMIT_TIMESTAMP}"
fi

# If:
#   - the version is a stable version and
#   - the current commit is on the `main` branch and
#   - it is the latest commit on the `main` branch
# then:
#   - add the `latest` tag to the Docker image tags
#   - add the `${MAVEN_ARTIFACT_VERSION_MAJOR}` tag to the Docker image tags
#   - add the `${MAVEN_ARTIFACT_VERSION_MAJOR}+${GIT_COMMIT_TIMESTAMP}` tag to the Docker image tags
#   - add the `${MAVEN_ARTIFACT_VERSION_MAJOR}.${MAVEN_ARTIFACT_VERSION_MINOR}` tag to the Docker image tags
#   - add the `${MAVEN_ARTIFACT_VERSION_MAJOR}.${MAVEN_ARTIFACT_VERSION_MINOR}+${GIT_COMMIT_TIMESTAMP}` tag to the Docker image tags
#   - add the `${MAVEN_ARTIFACT_VERSION_MAJOR}.${MAVEN_ARTIFACT_VERSION_MINOR}.${MAVEN_ARTIFACT_VERSION_PATCH}` tag to the Docker image tags
#   - add the `${MAVEN_ARTIFACT_VERSION_MAJOR}.${MAVEN_ARTIFACT_VERSION_MINOR}.${MAVEN_ARTIFACT_VERSION_PATCH}+${GIT_COMMIT_TIMESTAMP}` tag to the Docker image tags
if [[ "${MAVEN_IS_SNAPSHOT}" == "false" && "${GIT_IS_MAIN_BRANCH}" == "true" && "${GIT_IS_LATEST_MAIN_BRANCH_COMMIT}" == "true" ]]; then
  DOCKER_TAGS="${DOCKER_TAGS} latest"
  DOCKER_TAGS="${DOCKER_TAGS} ${MAVEN_ARTIFACT_VERSION_MAJOR}"
  DOCKER_TAGS="${DOCKER_TAGS} ${MAVEN_ARTIFACT_VERSION_MAJOR}+${GIT_COMMIT_TIMESTAMP}"
  DOCKER_TAGS="${DOCKER_TAGS} ${MAVEN_ARTIFACT_VERSION_MAJOR}.${MAVEN_ARTIFACT_VERSION_MINOR}"
  DOCKER_TAGS="${DOCKER_TAGS} ${MAVEN_ARTIFACT_VERSION_MAJOR}.${MAVEN_ARTIFACT_VERSION_MINOR}+${GIT_COMMIT_TIMESTAMP}"
  DOCKER_TAGS="${DOCKER_TAGS} ${MAVEN_ARTIFACT_VERSION_MAJOR}.${MAVEN_ARTIFACT_VERSION_MINOR}.${MAVEN_ARTIFACT_VERSION_PATCH}"
  DOCKER_TAGS="${DOCKER_TAGS} ${MAVEN_ARTIFACT_VERSION_MAJOR}.${MAVEN_ARTIFACT_VERSION_MINOR}.${MAVEN_ARTIFACT_VERSION_PATCH}+${GIT_COMMIT_TIMESTAMP}"
fi

# If:
#   - the version is a stable version
# then:
#   - determine if the Maven artifact is already published
# else:
#   - always publish the Maven artifact (because it is a SNAPSHOT version)
if [[ "${MAVEN_IS_SNAPSHOT}" == "false" ]]; then
  # Check if the Maven artifact is already published
  PUBLISH_MAVEN_ARTIFACT=$(
    ./mvnw dependency:get \
      "-Dartifact=${MAVEN_ARTIFACT_GROUP_ID}:${MAVEN_ARTIFACT_ID}:${MAVEN_ARTIFACT_VERSION}" \
      --quiet 2>/dev/null \
      && echo "false" \
      || echo "true"
  )
else
  PUBLISH_MAVEN_ARTIFACT=true
fi

# Trim leading and trailing whitespace from the Docker tags
DOCKER_TAGS=$(echo "${DOCKER_TAGS}" | xargs)

# Transform Docker tags into docker/metadata-action compatible format
DOCKER_METADATA_ACTION_TAGS=$(echo "${DOCKER_TAGS}" | tr ' ' '\n' | sed '/^$/d; s/^/type=raw,value=/')

# Output the artifact info
echo "${MESSAGE_COLOR}Git commit short hash:        ${VALUE_COLOR}${GIT_COMMIT_SHORT_HASH}${RESET_COLORS}"
echo "${MESSAGE_COLOR}Git commit long hash:         ${VALUE_COLOR}${GIT_COMMIT_LONG_HASH}${RESET_COLORS}"
echo "${MESSAGE_COLOR}Git commit timestamp:         ${VALUE_COLOR}${GIT_COMMIT_TIMESTAMP}${RESET_COLORS}"
echo "${MESSAGE_COLOR}Git is main branch:           ${VALUE_COLOR}${GIT_IS_MAIN_BRANCH}${RESET_COLORS}"
echo "${MESSAGE_COLOR}Git is latest main commit:    ${VALUE_COLOR}${GIT_IS_LATEST_MAIN_BRANCH_COMMIT}${RESET_COLORS}"
echo "${MESSAGE_COLOR}Maven artifact Group ID:      ${VALUE_COLOR}${MAVEN_ARTIFACT_GROUP_ID}${RESET_COLORS}"
echo "${MESSAGE_COLOR}Maven artifact ID:            ${VALUE_COLOR}${MAVEN_ARTIFACT_ID}${RESET_COLORS}"
echo "${MESSAGE_COLOR}Maven artifact Version:       ${VALUE_COLOR}${MAVEN_ARTIFACT_VERSION}${RESET_COLORS}"
echo "${MESSAGE_COLOR}Maven artifact Version major: ${VALUE_COLOR}${MAVEN_ARTIFACT_VERSION_MAJOR}${RESET_COLORS}"
echo "${MESSAGE_COLOR}Maven artifact Version minor: ${VALUE_COLOR}${MAVEN_ARTIFACT_VERSION_MINOR}${RESET_COLORS}"
echo "${MESSAGE_COLOR}Maven artifact Version patch: ${VALUE_COLOR}${MAVEN_ARTIFACT_VERSION_PATCH}${RESET_COLORS}"
echo "${MESSAGE_COLOR}Maven is SNAPSHOT:            ${VALUE_COLOR}${MAVEN_IS_SNAPSHOT}${RESET_COLORS}"
echo "${MESSAGE_COLOR}Publish Maven artifact:       ${VALUE_COLOR}${PUBLISH_MAVEN_ARTIFACT}${RESET_COLORS}"
echo "${MESSAGE_COLOR}Docker tags:                  ${VALUE_COLOR}${DOCKER_TAGS}${RESET_COLORS}"

# Output variables for use in subsequent steps
# shellcheck disable=SC2129
echo "git-commit-short-hash=${GIT_COMMIT_SHORT_HASH}"                >> "${GITHUB_OUTPUT}"
echo "git-commit-long-hash=${GIT_COMMIT_LONG_HASH}"                  >> "${GITHUB_OUTPUT}"
echo "git-commit-timestamp=${GIT_COMMIT_TIMESTAMP}"                  >> "${GITHUB_OUTPUT}"
echo "git-is-main-branch=${GIT_IS_MAIN_BRANCH}"                      >> "${GITHUB_OUTPUT}"
echo "git-is-latest-main-commit=${GIT_IS_LATEST_MAIN_BRANCH_COMMIT}" >> "${GITHUB_OUTPUT}"
echo "maven-artifact-group-id=${MAVEN_ARTIFACT_GROUP_ID}"            >> "${GITHUB_OUTPUT}"
echo "maven-artifact-id=${MAVEN_ARTIFACT_ID}"                        >> "${GITHUB_OUTPUT}"
echo "maven-artifact-version=${MAVEN_ARTIFACT_VERSION}"              >> "${GITHUB_OUTPUT}"
echo "maven-artifact-version-major=${MAVEN_ARTIFACT_VERSION_MAJOR}"  >> "${GITHUB_OUTPUT}"
echo "maven-artifact-version-minor=${MAVEN_ARTIFACT_VERSION_MINOR}"  >> "${GITHUB_OUTPUT}"
echo "maven-artifact-version-patch=${MAVEN_ARTIFACT_VERSION_PATCH}"  >> "${GITHUB_OUTPUT}"
echo "maven-is-snapshot=${MAVEN_IS_SNAPSHOT}"                        >> "${GITHUB_OUTPUT}"
echo "maven-artifact-publish=${PUBLISH_MAVEN_ARTIFACT}"              >> "${GITHUB_OUTPUT}"
echo "docker-tags=${DOCKER_TAGS}"                                    >> "${GITHUB_OUTPUT}"

{
  echo "## Prepare Build Environment Summary"
  echo ""
  echo "|  Output                          | Value                                   |"
  echo "|----------------------------------|-----------------------------------------|"
  echo "| \`git-commit-short-hash\`        | \`${GIT_COMMIT_SHORT_HASH}\`            |"
  echo "| \`git-commit-long-hash\`         | \`${GIT_COMMIT_LONG_HASH}\`             |"
  echo "| \`git-commit-timestamp\`         | \`${GIT_COMMIT_TIMESTAMP}\`             |"
  echo "| \`git-is-main-branch\`           | \`${GIT_IS_MAIN_BRANCH}\`               |"
  echo "| \`git-is-latest-main-commit\`    | \`${GIT_IS_LATEST_MAIN_BRANCH_COMMIT}\` |"
  echo "| \`maven-artifact-group-id\`      | \`${MAVEN_ARTIFACT_GROUP_ID}\`          |"
  echo "| \`maven-artifact-id\`            | \`${MAVEN_ARTIFACT_ID}\`                |"
  echo "| \`maven-artifact-version\`       | \`${MAVEN_ARTIFACT_VERSION}\`           |"
  echo "| \`maven-artifact-version-major\` | \`${MAVEN_ARTIFACT_VERSION_MAJOR}\`     |"
  echo "| \`maven-artifact-version-minor\` | \`${MAVEN_ARTIFACT_VERSION_MINOR}\`     |"
  echo "| \`maven-artifact-version-patch\` | \`${MAVEN_ARTIFACT_VERSION_PATCH}\`     |"
  echo "| \`maven-is-snapshot\`            | \`${MAVEN_IS_SNAPSHOT}\`                |"
  echo "| \`maven-artifact-publish\`       | \`${PUBLISH_MAVEN_ARTIFACT}\`           |"
  echo "| \`docker-tags\`                  | \`${DOCKER_TAGS}\`                      |"
} >> "${GITHUB_STEP_SUMMARY:-/dev/null}"


DOCKER_METADATA_ACTION_TAGS_OUTPUT_DELIMITER="__DOCKER_METADATA_ACTION_TAGS__"
{
  echo "docker-metadata-action-tags<<${DOCKER_METADATA_ACTION_TAGS_OUTPUT_DELIMITER}"
  printf '%s\n' "${DOCKER_METADATA_ACTION_TAGS}"
  echo "${DOCKER_METADATA_ACTION_TAGS_OUTPUT_DELIMITER}"
} >> "${GITHUB_OUTPUT}"
