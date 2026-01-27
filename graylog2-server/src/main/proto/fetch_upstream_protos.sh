#!/bin/bash
#
# Downloads proto specs from upstream repositories.
# Update the versions below, then run this script.
#
# Note: Requires GNU tar (for --wildcards --no-anchored options).
# On macOS: brew install gnu-tar && TAR_BIN=gtar ./update_proto_specs.sh
#
set -eo pipefail

# OpenTelemetry Proto - https://github.com/open-telemetry/opentelemetry-proto/releases
OTEL_VERSION="v1.5.0"

# OpAMP Proto - https://github.com/open-telemetry/opamp-spec/releases
OPAMP_VERSION="v0.14.0"

# ---

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TAR_BIN="${TAR_BIN:-tar}"

echo "Downloading OpenTelemetry proto $OTEL_VERSION"
[ -d "$SCRIPT_DIR/opentelemetry" ] && rm -r "$SCRIPT_DIR/opentelemetry"
mkdir -p "$SCRIPT_DIR/opentelemetry"
curl -sL "https://github.com/open-telemetry/opentelemetry-proto/archive/refs/tags/${OTEL_VERSION}.tar.gz" \
    | $TAR_BIN -xzf - --strip-components=2 --wildcards --no-anchored -C "$SCRIPT_DIR/opentelemetry" '*.proto'

echo "Downloading OpAMP proto $OPAMP_VERSION"
[ -d "$SCRIPT_DIR/opamp" ] && rm -r "$SCRIPT_DIR/opamp"
mkdir -p "$SCRIPT_DIR/opamp"
curl -sL "https://github.com/open-telemetry/opamp-spec/archive/refs/tags/${OPAMP_VERSION}.tar.gz" \
    | $TAR_BIN -xzf - --strip-components=1 --wildcards --no-anchored -C "$SCRIPT_DIR/opamp" '*.proto'

echo "Done"
