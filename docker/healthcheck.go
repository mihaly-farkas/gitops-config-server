// Package main implements a lightweight, high-performance health check client for containerized environments.
//
// It connects to the Spring Boot application over a Unix domain socket and verifies its operational readiness based on
// a single-byte response.
//
// # Exit Codes
//
// The program signals the health status using standard POSIX exit codes:
//   - 0: Healthy ('H' received)
//   - 1: Command-line argument error (invalid number of arguments)
//   - 2: Connection failed (socket unavailable or permission denied)
//   - 3: Timeout or failed to set connection deadlines
//   - 4: Protocol error (invalid response length or read error)
//   - 5: Unhealthy ('U' or any unexpected byte received)
package main

import (
	"net"
	"os"
	"time"
)

func main() {
	// Validate command-line arguments.
	if len(os.Args) != 2 {
		os.Exit(1)
	}

	// Extract the Unix socket path from the command-line arguments.
	socketPath := os.Args[1]

	// Connect to the Unix socket with a timeout of 1 second.
	conn, err := net.DialTimeout("unix", socketPath, time.Second)
	if err != nil {
		os.Exit(2)
	}

	// Ensure the connection is closed when the function exits.
	defer func() {
		_ = conn.Close()
	}()

	// Set a read deadline of 1 second.
	if err := conn.SetReadDeadline(time.Now().Add(time.Second)); err != nil {
		os.Exit(3)
	}

	// Read a single byte from the connection.
	var response [1]byte
	n, err := conn.Read(response[:])
	if err != nil || n != 1 {
		os.Exit(4)
	}

	// H : Healthy, U : Unhealthy
	if response[0] != 'H' {
		os.Exit(5)
	}
}
