#!/usr/bin/env bash

# ANSI color escape sequences.
export BLACK=$'\e[0;30m'
export RED=$'\e[0;31m'
export GREEN=$'\e[0;32m'
export YELLOW=$'\e[0;33m'
export BLUE=$'\e[0;34m'
export MAGENTA=$'\e[0;35m'
export CYAN=$'\e[0;36m'
export WHITE=$'\e[0;37m'

# Bright ANSI color escape sequences.
export BRIGHT_BLACK=$'\e[1;30m'
export BRIGHT_RED=$'\e[1;31m'
export BRIGHT_GREEN=$'\e[1;32m'
export BRIGHT_YELLOW=$'\e[1;33m'
export BRIGHT_BLUE=$'\e[1;34m'
export BRIGHT_MAGENTA=$'\e[1;35m'
export BRIGHT_CYAN=$'\e[1;36m'
export BRIGHT_WHITE=$'\e[1;37m'

# Reset all ANSI attributes.
export RESET_COLORS=$'\e[0m'

# Named colors
export MESSAGE_COLOR="${BRIGHT_CYAN}"
export VALUE_COLOR="${BRIGHT_MAGENTA}"
export OK_COLOR="${BRIGHT_GREEN}"
export WARNING_COLOR="${BRIGHT_YELLOW}"
export ERROR_COLOR="${BRIGHT_RED}"
