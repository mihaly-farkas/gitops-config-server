#!/bin/sh

set -eu

if [ "${CONFIG_GIT_URI}" = "" ]; then
  echo "ERROR: Required variable CONFIG_GIT_URI is missing" >&2
  exit 1
fi

APP_VERSION=$(cat APP_VERSION)

echo "Starting Spring Cloud Config Server v${APP_VERSION}"
exec java -jar "spring-cloud-config-server-${APP_VERSION}.jar"

