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

Changed fields:
- `message`: Now contains the full JSON content of the log message. The `vendor_event_description` field now contains the previous `message` field value for backwards-compatibility. The full message can still also optionally be stored in the `full_message` field if the `Store full message` option is enabled.
- The message `timestamp` field is now set to the current Graylog system date/time, instead of the previously used log `time` value. The `event_created` field now contains the previous `time` value for backwards-compatibility.

Added fields:
`vendor_event_description`: Contains the value which was previously present in the `message` log field.
`event_created`: Contains the `time` log value.
`event_source_input`: Contains the static value `aws_security_lake`.
`vendor_version`: Contains the `metadata.product.version` log value.

Note that additional AWS Security Lake message parsing is expected to be released in an upcoming release of Graylog Illuminate.

## Office 365 input log parsing changes

Several log parsing changes have been made to the Office 365 input in preparation for Illuminate parsing content.

Changed fields:
- `message`: Now contains the full JSON content of the log message. The `vendor_event_description` field now contains the previous `message` field value for backwards-compatibility. The full message can still also optionally be stored in the `full_message` field if the `Store full message` option is enabled.
- The message `timestamp` field is now set to the current Graylog system date/time, instead of the previously used log `CreationTime` value. The `event_created` field now contains the previous `CreationTime` value for backwards-compatibility.
- `vendor_event_description`: Now contains the value which was previously present in the `message` log field.

Added fields:
- `event_created`: Contains the `CreationTime` log value.
- `event_source_product`: Contains the static value `o365`.
- `vendor_version`: Contains the `Version` log value.
- `vendor_subtype`: Contains the `Workload` log value.

Note that additional Office 365 message parsing is expected to be released in an upcoming release of Graylog Illuminate.

## Okta Log Events input log parsing changes

Several log parsing changes have been made to the Okta Log Events input in preparation for Illuminate parsing content.

Changed fields:
- `message`: Now contains the full JSON content of the log message. The `vendor_event_description` field now contains the previous `message` field value for backwards-compatibility. The full message can still also optionally be stored in the `full_message` field if the `Store full message` option is enabled.
- The message `timestamp` field is now set to the current Graylog system date/time, instead of the previously used log `published` value. The `event_created` field now contains the previous `published` value for backwards-compatibility.
- `vendor_event_description`: Now contains the value which was previously present in the `message` log field.

Added fields:
- `event_created`: Contains the `published` log value.
- `event_source_product`: Contains the static value `okta`.
- `vendor_version`: Contains the `version` log value.
- `vendor_subtype`: Contains the `eventType` log value.

Note that additional Okta Log Events message parsing is expected to be released in an upcoming release of Graylog Illuminate.

## F5 BIG-IP input log parsing changes

Several log parsing changes have been made to the F5 BIG-IP input in preparation for Illuminate parsing content. The full message can still also optionally be stored in the `full_message` field if the `Store full message` option is enabled.

Changed fields:
- `message`: Now contains the full JSON content of the log message. The `vendor_event_description` field now contains the previous `message` field value for backwards-compatibility.
- The message `timestamp` field is now set to the current Graylog system date/time, instead of the previously used log `vendorTimestamp`, `eventCreated`, or `timestamp` values. The `event_created` field now contains the previous `vendorTimestamp`, `eventCreated`, or `timestamp` value for backwards-compatibility.
- `source`: Now contains the `host` log value if present, or the static value `F5 BIG-IP` used previously if not.
- `vendor_event_description`: Now contains the value which was previously present in the `message` log field.

Added fields:
- `event_created`: Contains the `vendorTimestamp`, `eventCreated`, or `timestamp` log value.
- `event_source_product`: Contains the static value `f5_big-ip`.

Note that additional F5 BIG-IP message parsing is expected to be released in an upcoming release of Graylog Illuminate.

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
