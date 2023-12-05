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

## Configuration File Changes
| Option                                         | Action    | Description                                                                                                                                                                                                                                             |
|------------------------------------------------|-----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `disabled_retention_strategies`  | **added** | Disables the specified retention strategies. By default, strategies `none` and `close` are now disabled in new installations.<br/>Strategies can be re-enabled simply by removing from this list.<br/>**Do not extend this list on existing installs!** |


## Asset Import Changes

Graylog 5.2 introduced the Assets feature and the ability to import Assets from Active Directory.
Previously Graylog users could define any AD attribute to map to a Graylog Asset's User ID field.

This functionality has been amended to only allow the Active Directory SID attribute for AD User Asset import mapping configurations,
to better align with the GIM schema and allow for targeted handling of AD SIDs.

Any existing Active Directory User Asset import configurations will be automatically updated to use the SID as the Unique ID attribute, potentially changing the behavior of subsequent imports by those configurations.

## Java API Changes

The following Java Code API changes have been made.

| File/method                    | Description               |
|--------------------------------|---------------------------|
| `org.graylog2.plugin.Message#addStringFields` | Deprecated method removed |
| `org.graylog2.plugin.Message#addLongFields` | Deprecated method removed |
| `org.graylog2.plugin.Message#addDoubleFields` | Deprecated method removed |
| `org.graylog2.plugin.Message#getValidationErrors` | Deprecated method removed |


## REST API Endpoint Changes

The following REST API changes have been made.

| Endpoint                | Description              |
|-------------------------|--------------------------|
| `GET /example/resource` | TODO placeholder comment |
