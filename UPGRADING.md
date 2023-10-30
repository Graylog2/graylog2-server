Upgrading to Graylog 6.0.x
==========================

## Breaking Changes

### Prometheus metrics

The name of the `jvm_classes_loaded` metric [has been changed](https://github.com/prometheus/client_java/pull/681).

Prometheus queries referencing `jvm_classes_loaded` need to be adapted to
the new name `jvm_classes_currently_loaded`.

## Java API Changes

The following Java Code API changes have been made.

| File/method                   | Description              |
|-------------------------------|--------------------------|
| `ExampleClass#exampleFuntion` | TODO placeholder comment |

## REST API Endpoint Changes

The following REST API changes have been made.

| Endpoint                | Description              |
|-------------------------|--------------------------|
| `GET /example/resource` | TODO placeholder comment |
