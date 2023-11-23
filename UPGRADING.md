Upgrading to Graylog 6.0.x
==========================

## Breaking Changes

### Prometheus metrics

The name of the `jvm_classes_loaded` metric [has been changed](https://github.com/prometheus/client_java/pull/681).

Prometheus queries referencing `jvm_classes_loaded` need to be adapted to
the new name `jvm_classes_currently_loaded`.

### Plugins

Removal of `systemnavigation` web interface plugin. Previously it was possible to register options for the
system dropdown in the navigation, by using the `systemnavigation` plugin.
Now this can be achieved by registering a `navigation` plugin.
The plugin entity needs the `description` `System` and `children` (array).
Every child represents a dropdown option and needs a `path` and `description` attribute.


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
