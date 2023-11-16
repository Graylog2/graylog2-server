Upgrading to Graylog 6.0.x
==========================

## Breaking Changes

### Prometheus metrics

The name of the `jvm_classes_loaded` metric [has been changed](https://github.com/prometheus/client_java/pull/681).

Prometheus queries referencing `jvm_classes_loaded` need to be adapted to
the new name `jvm_classes_currently_loaded`.

## Asset Import Changes

Graylog 5.2 introduced the Assets feature and the ability to import Assets from Active Directory.
Previously Graylog users could define any AD attribute to map to a Graylog Asset's User ID field.

This functionality has been amended to only allow the Active Directory SID attribute for AD User Asset import mapping configurations,
to better align with the GIM schema and allow for targeted handling of AD SIDs.

Any existing Active Directory User Asset import configurations will be automatically updated to use the SID as the Unique ID attribute, potentially changing the behavior of subsequent imports by those configurations.

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
