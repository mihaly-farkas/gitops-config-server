/**
 * Provides a lightweight health check mechanism for containerized deployments.
 *
 * <p>This package implements a Unix domain socket-based health server that exposes application
 * health status with minimal overhead. It is designed for hardened container images where standard
 * HTTP tools may not be available.
 */
package io.github.mihaly_farkas.gitops_config_server.feat.health_socket_server;
