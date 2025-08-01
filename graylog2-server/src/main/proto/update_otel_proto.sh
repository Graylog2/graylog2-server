#!/bin/bash
set -eo pipefail

if [ -z "$1" ]; then
  echo "Usage: $0 <version>"
  echo "Example: $0 v1.7.0"
  exit 1
fi

VERSION="$1"
ARCHIVE_URL="https://github.com/open-telemetry/opentelemetry-proto/archive/refs/tags/${VERSION}.tar.gz"

# Use TAR_BIN from env if set, otherwise default to 'tar'
TAR_BIN="${TAR_BIN:-tar}"

echo "Fetching and extracting .proto files from: $ARCHIVE_URL"

# Stream and extract only .proto files, preserving directory structure
curl -sL "$ARCHIVE_URL" | $TAR_BIN --strip-components=1 -xvzf - --wildcards --no-anchored '*.proto'

echo "Extraction complete."
