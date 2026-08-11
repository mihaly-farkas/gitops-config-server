#!/bin/sh

# Set strict mode for bash
set -euo

# If the Git URI is not set, exit with an error
if [ "${CONFIG_GIT_URI:-}" = "" ]; then
  echo "ERROR: Required variable CONFIG_GIT_URI is missing" >&2
  exit 1
fi

# If the Spring Security user name is not set, exit with an error
if [ "${CONFIG_ADMIN_USER_NAME:-}" = "" ]; then
  echo "ERROR: Required variable CONFIG_ADMIN_USER_NAME is missing" >&2
  exit 1
fi

# If the Spring Security user password is not set, exit with an error
if [ "${CONFIG_ADMIN_PASSWORD:-}" = "" ]; then
  echo "ERROR: Required variable CONFIG_ADMIN_PASSWORD is missing" >&2
  exit 1
fi

# Create the application-docker.properties file with the required configuration
{
  echo "spring.security.user.name=\${CONFIG_ADMIN_USER_NAME}"
  echo "spring.security.user.password=\${CONFIG_ADMIN_PASSWORD}"
  echo "spring.cloud.config.server.git.uri=\${CONFIG_GIT_URI}"
} >> application-docker.properties

# If the encryption key is set, add it to the application-docker.properties file
if [ "${CONFIG_ENCRYPTION_KEY:-}" != "" ]; then
  echo "encrypt.key=\${CONFIG_ENCRYPTION_KEY}" >> application-docker.properties
fi

# Start the GitOps Config Server with the external `application-docker.properties` config and the `docker` profile
exec java -jar "gitops-config-server-${APP_VERSION}.jar" \
  --spring.config.additional-location=file:application-docker.properties \
  --spring.profiles.active=docker
