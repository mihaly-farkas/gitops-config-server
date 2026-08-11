# syntax=docker/dockerfile:1.26
FROM eclipse-temurin:25-jre-alpine

ARG APP_VERSION

ENV APP_VERSION=${APP_VERSION}
ENV SERVER_PORT=8888
ENV CONFIG_ADMIN_USER_NAME="admin"
ENV CONFIG_GIT_DEFAULT_LABEL="main"
ENV CONFIG_REFRESH_RATE=0
ENV MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE="health"

WORKDIR /app

COPY target/gitops-config-server-${APP_VERSION}.jar ./
COPY docker/entrypoint.sh ./

RUN chmod +x entrypoint.sh

EXPOSE 8888

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
	CMD wget --quiet \
	  --header="Authorization: Basic $(printf "%s:%s" "${CONFIG_ADMIN_USER_NAME}" "${CONFIG_ADMIN_PASSWORD}" | base64)" \
	  --output-document=/dev/null \
	  "http://127.0.0.1:${SERVER_PORT}/actuator/health" || exit 1

# See the entrypoint.sh at: https://github.com/mihaly-farkas/gitops-config-server/blob/main/docker/entrypoint.sh
ENTRYPOINT ["./entrypoint.sh"]

