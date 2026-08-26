# Protobuf files

The `.proto` files in this directory and its subdirectories are used to generate Java source files
during the Maven build process.

## OpenTelemetry

We maintain a copy of the protobuf specs from the
[OpenTelemetry Protocol (OTLP) Specification](https://github.com/open-telemetry/opentelemetry-proto) repository.

These are required as dependencies for the `otel-raw-journal-record.proto` spec.

## OpAMP

We also maintain a copy of the protobuf specs from the
[OpAMP Specification](https://github.com/open-telemetry/opamp-spec) repository.

These are used for the OpAMP-based sidecar communication.

## Updating the specs

To update the proto specs, edit the version numbers at the top of the `fetch_upstream_protos.sh` script
and then run it:

```bash
./fetch_upstream_protos.sh
```

The script will download the release archives and extract the `.proto` files into `./opentelemetry`
and `./opamp`, maintaining the directory structure.

> **Note**: The script requires GNU tar. On macOS, install it with `brew install gnu-tar` and run:
> ```bash
> TAR_BIN=gtar ./fetch_upstream_protos.sh
> ```
