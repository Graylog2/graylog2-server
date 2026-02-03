# Protobuf files

The `.proto` files in this directory and its subdirectories are used to generate Java source files
during the maven build process.

## OpenTelemetry

Along with our own protobuf specs, we maintain a copy of the protobuf specs from the 
[OpenTelemetry Protocol (OTLP) Specification](https://github.com/open-telemetry/opentelemetry-proto) repository. 

These are required as dependencies for the `otel-raw-journal-record.proto` spec.

To update the opentelemetry specs, find the desired version at the [available releases](https://github.com/open-telemetry/opentelemetry-proto/releases)
page and then run the `update_otel_proto.sh` script.

This will download the release archive and extract the `.proto` files into `./opentelemetry`, maintaining the directory
structure.

>**CAVEAT**: The script requires GNU tar in order to work correctly. On a Mac you might have to install it first. You
> can specify a different tar command to be used by the script by setting the `TAR_BIN` environment variable.

A typical update process would look like this:

```bash
rm -r opentelemetry
./update_otel_proto.sh v1.7.0
```
