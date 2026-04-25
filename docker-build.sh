#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IMAGE_NAME="patch-minder-builder"

# Build the Docker image (cached after first run)
docker build -t "$IMAGE_NAME" "$SCRIPT_DIR"

# Run the build, mounting gradle cache for speed
docker run --rm \
  -v "$SCRIPT_DIR":/project \
  -v patch-minder-gradle-cache:/root/.gradle \
  -w /project \
  "$IMAGE_NAME" \
  "$@"
