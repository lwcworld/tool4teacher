#!/bin/bash

# submodule_clone.sh
# Usage: ./submodule_clone.sh third_party/<repo_name>

set -e

# Check if argument is provided
if [ -z "$1" ]; then
    echo "Error: No submodule path provided"
    echo "Usage: $0 third_party/<repo_name>"
    exit 1
fi

SUBMODULE_PATH="$1"

# Check if the path exists in .gitmodules
if ! git config -f .gitmodules --get "submodule.${SUBMODULE_PATH}.path" > /dev/null 2>&1; then
    echo "Error: '${SUBMODULE_PATH}' is not a registered submodule"
    echo ""
    echo "Available submodules:"
    git config -f .gitmodules --get-regexp path | awk '{ print "  - " $2 }'
    exit 1
fi

echo "Initializing and cloning submodule: ${SUBMODULE_PATH}"

# Initialize and update the specific submodule recursively
git submodule update --init --recursive "${SUBMODULE_PATH}"

echo ""
echo "Successfully cloned submodule: ${SUBMODULE_PATH}"
