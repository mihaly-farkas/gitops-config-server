# syntax=docker/dockerfile:1.26
FROM golang:1.27-alpine AS healthcheck-builder

WORKDIR /builder

COPY docker/healthcheck.go .

RUN CGO_ENABLED=0 \
    go build \
    -trimpath \
    -ldflags="-s -w" \
    -o healthcheck \
    healthcheck.go

FROM dhi.io/eclipse-temurin:25.0.4.7-alpine3.24

ARG APP_VERSION
ARG BUILD_ID

ENV APP_VERSION=${APP_VERSION}
ENV SERVER_PORT=8888

USER nonroot

WORKDIR /app

COPY --from=healthcheck-builder /builder/healthcheck /app/healthcheck
COPY target/spring-boot-config-server-${APP_VERSION}.jar ./spring-boot-config-server.jar

ENTRYPOINT ["java", \
  "-jar", "/app/spring-boot-config-server.jar", \
  "--spring.profiles.active=docker", \
  "--mihaly-farkas.spring-boot-config-server.health.socket.enabled=true", \
  "--mihaly-farkas.spring-boot-config-server.health.socket.path=/tmp/health.sock" \
]

EXPOSE ${SERVER_PORT}

HEALTHCHECK \
  --start-interval=2s \
  --start-period=30s \
  --interval=30s \
  --timeout=2s \
  --retries=3 \
  CMD ["/app/healthcheck", "/tmp/health.sock"]
