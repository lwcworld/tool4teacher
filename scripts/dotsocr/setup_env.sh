#!/bin/bash

# Export environment variables from .env file
# Usage: source setup_env.sh

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${SCRIPT_DIR}/.env"

# Check if .env file exists
if [ ! -f "$ENV_FILE" ]; then
    echo "Error: .env file not found at $ENV_FILE"
    return 1 2>/dev/null || exit 1
fi

# Read and export variables from .env file
# Skip empty lines and comments (lines starting with #)
while IFS= read -r line || [ -n "$line" ]; do
    # Skip empty lines
    if [ -z "$line" ]; then
        continue
    fi

    # Skip comments
    if [[ "$line" =~ ^[[:space:]]*# ]]; then
        continue
    fi

    # Export the variable if it matches the pattern KEY=VALUE
    if [[ "$line" =~ ^[[:space:]]*([A-Za-z_][A-Za-z0-9_]*)=(.*)$ ]]; then
        export "${BASH_REMATCH[1]}=${BASH_REMATCH[2]}"
        echo "export ${BASH_REMATCH[1]}=${BASH_REMATCH[2]}"
    fi
done < "$ENV_FILE"
