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

## Asset Import Changes

Graylog 5.2 introduced the Assets feature and the ability to import Assets from Active Directory.
Previously Graylog users could define any AD attribute to map to a Graylog Asset's User ID field.

This functionality has been amended to only allow the Active Directory SID attribute for AD User Asset import mapping configurations,
to better align with the GIM schema and allow for targeted handling of AD SIDs.

Any existing Active Directory User Asset import configurations will be automatically updated to use the SID as the Unique ID attribute, potentially changing the behavior of subsequent imports by those configurations.

## AWS Security Lake input log parsing changes

Several log parsing changes have been made to the AWS Security Lake input in preparation for Illuminate parsing content.

Added fields:
`event_created`: Contains the `time` log value.
`event_input_source`: Contains the static value `aws_security_lake`.
`event_source_product`: Contains the `metadata.product.name` log value.
`vendor_version`: Contains the `metadata.product.versoin` log value.
`vendor_subtype`: Contains the `class_name` log value.


Changed fields:
- `message`: Now contains the full JSON content of the log message. 
- The message `timestamp` field is now set to the current Graylog system date/time, instead of the previously used log `time` value. The `event_created` field now contains the previous `time` value.

Removed fields:
- `answers`
- `api`
- `class_name` (This field stored in  `vendor_subtype`)
- `cloud`
- `compliance`
- `confidence`
- `connection_info`
- `destination_ip`
- `destination_port`
- `destination_subnet_id`
- `destination_vpc_id`
- `event_action,`
- `event_end`
- `event_log_name`
- `event_severity`
- `event_start`
- `finding`
- `http_request`
- `identity`
- `malware`
- `process`
- `query`
- `rcode`
- `source_ip`
- `source_port`
- `source_subnet_id`
- `source_vpc_id`
- `traffic`
- `vulnerabilities`

Note that additional AWS Security Lake message parsing is expected to be released in an upcoming release of Graylog Illuminate.

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
