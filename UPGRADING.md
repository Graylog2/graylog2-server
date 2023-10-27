Upgrading to Graylog 6.0.x 
==========================

## Breaking Changes

### Prometheus metrics
The name of the `jvm_classes_loaded` metric [has been changed](https://github.com/prometheus/client_java/pull/681).

Prometheus queries referencing `jvm_classes_loaded` need to be adapted to
the new name `jvm_classes_currently_loaded`.
