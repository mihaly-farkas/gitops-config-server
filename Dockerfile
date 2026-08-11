# syntax=docker/dockerfile:1.7

# Build stage
FROM eclipse-temurin:25-jdk AS builder

ARG SONAR_ORGANIZATION="mihaly-farkas"
ARG SONAR_PROJECT_KEY="mihaly-farkas_spring-cloud-config-server"
ARG SONAR_TOKEN=""
ARG MAVEN_OPTS=""

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

RUN chmod +x mvnw

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw --batch-mode --quiet dependency:go-offline

COPY .git .git
COPY src src


RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw --quiet -DforceStdout help:evaluate -Dexpression=project.version > /tmp/APP_VERSION

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw --batch-mode spotless:check verify sonar:sonar \
    "-Dsonar.organization=${SONAR_ORGANIZATION}" \
    "-Dsonar.projectKey=${SONAR_PROJECT_KEY}" \
    "-Dsonar.sources=src/main" \
    "-Dsonar.coverage.exclusions=src/main/java/io/github/mihaly_farkas/spring_cloud_config_server/SpringCloudConfigServer.java" \
    ${MAVEN_OPTS}

# Final stage
FROM eclipse-temurin:25-jre-alpine

ENV SERVER_PORT=8888
ENV CONFIG_GIT_URI=""
ENV CONFIG_GIT_USERNAME=""
ENV CONFIG_GIT_PASSWORD=""
ENV CONFIG_GIT_DEFAULT_LABEL="main"
ENV CONFIG_REFRESH_RATE=-1
ENV CONFIG_ENCRYPTION_KEY=""
ENV SPRING_SECURITY_USER_NAME="admin"
ENV SPRING_SECURITY_USER_PASSWORD=""

WORKDIR /app

COPY --from=builder /workspace/target/spring-cloud-config-server-*.jar ./
COPY --from=builder /tmp/APP_VERSION ./
COPY docker/entrypoint.sh ./

RUN chmod +x entrypoint.sh

EXPOSE 8888

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
	CMD wget -q -O /dev/null "http://127.0.0.1:${SERVER_PORT}/actuator/health" || exit 1

ENTRYPOINT ["./entrypoint.sh"]

