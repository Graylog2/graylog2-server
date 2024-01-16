Upgrading to Graylog 6.0.x
==========================

## Breaking Changes

### Changed default number of process-buffer and output-buffer processors

The default values for the configuration settings `processbuffer_processors` and `outputbuffer_processors` have been
changed. The values will now be calculated based on the number of CPU cores available to the JVM. If you have not
explicitly set values for these settings in your configuration file, the new defaults apply.

The new defaults should improve performance of your system, however, if you want to continue running your system with
the previous defaults, please add the following settings to your configuration file:

```
processbuffer_processors = 5
outputbuffer_processors = 3
```

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

## Input log parsing changes

Log parsing changes have been made several inputs in preparation for Illuminate parsing content. Note that additional 
message parsing for these inputs is expected to be released in an upcoming release of Graylog Illuminate.

### AWS Security Lake input

Changed fields:
- `message`: Now contains the full JSON content of the log message. 
- The message `timestamp` field is now set to the current Graylog system date/time, instead of the previously used log `time` value. The `event_created` field now contains the previous `time` value for backwards-compatibility.

Added fields:
- `event_created`: Contains the `time` log value.
- `event_source_input`: Contains the static value `aws_security_lake`.
- `vendor_event_type`: Contains the `type_name` log value (previously in the `event_log_name` field).
- `vendor_event_severity`: Contains the `severity` log value (previously in the `event_severity` field).
- `vendor_version`: Contains the `metadata.product.version` log value.

Removed fields:
- `answers`
- `api`
- `class_name` (this value is still available in the `source` field)
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

### Office 365 input

Changed fields:
- `message`: Now contains the full JSON content of the log message. The `vendor_event_description` field now contains the previous `message` field value for backwards-compatibility. 
- The message `timestamp` field is now set to the current Graylog system date/time, instead of the previously used log `CreationTime` value. The `event_created` field now contains the previous `CreationTime` value for backwards-compatibility.
- `vendor_event_description`: Now contains the value which was previously present in the `message` log field.

Added fields:
- `event_created`: Contains the `CreationTime` log value.
- `event_source_product`: Contains the static value `o365`.
- `vendor_subtype`: Contains the `Workload` log value.
- `vendor_version`: Contains the `Version` log value.

### Okta Log Events input

Several log parsing changes have been made to the Okta Log Events input in preparation for Illuminate parsing content.

Changed fields:
- `message`: Now contains the full JSON content of the log message. The `vendor_event_description` field now contains the previous `message` field value for backwards-compatibility. 
- The message `timestamp` field is now set to the current Graylog system date/time, instead of the previously used log `published` value. The `event_created` field now contains the previous `published` value for backwards-compatibility.
- `vendor_event_description`: Now contains the value which was previously present in the `message` log field.

Added fields:
- `event_created`: Contains the `published` log value.
- `event_source_product`: Contains the static value `okta`.
- `vendor_event_type`: Contains the `eventType` log value.
- `vendor_version`: Contains the `version` log value.

### F5 BIG-IP input

Changed fields:
- `message`: Now contains the full text content of the log message. The `vendor_event_description` field now contains the previous `message` field value for backwards-compatibility.
- The message `timestamp` field is now set to the current Graylog system date/time, instead of the previously used log `vendorTimestamp`, `eventCreated`, or `timestamp` values. The `event_created` field now contains the previous `vendorTimestamp`, `eventCreated`, or `timestamp` value for backwards-compatibility.
- `source`: Now contains the `host` log value if present, or the static value `F5 BIG-IP` used previously if not.
- `vendor_event_description`: Now contains the value which was previously present in the `message` log field.

Added fields:
- `event_created`: Contains the `vendorTimestamp`, `eventCreated`, or `timestamp` log value.
- `event_source_product`: Contains the static value `f5_big-ip`.

Removed fields:
- `host_name` (this value is still available in the `source` field)
- `log_level`
- `service`
- `vendor_event_description`

## Java API Changes

The following Java Code API changes have been made.

| File/method                                                           | Description                    |
|-----------------------------------------------------------------------|--------------------------------|
| `org.graylog2.plugin.Message#addStringFields`                         | Deprecated method removed      |
| `org.graylog2.plugin.Message#addLongFields`                           | Deprecated method removed      |
| `org.graylog2.plugin.Message#addDoubleFields`                         | Deprecated method removed      |
| `org.graylog2.plugin.Message#getValidationErrors`                     | Deprecated method removed      |
| `org.graylog2.plugin.SingletonMessages`                               | Unused class removed           |
| `org.graylog.plugins.views.search.engine.LuceneQueryParsingException` | Unused exception class removed |
| `org.graylog2.indexer.IndexMappingTemplate#toTemplate`                | Method parameter list modified |

### Transition from the `javax` to the `jakarta` namespace

Graylog was using various annotations from the `javax.*` packages, e.g. to annotate REST
resources or to facilitate dependency injection. The package name for some of these
annotations has been changed to `jakarta.*`. For a plugin to keep working as expected,
its code needs to be adjusted to also use the new package names.

| previous name               | new name                      |
|-----------------------------|-------------------------------|
| `javax.annotation.Priority` | `jakarta.annotation.Priority` |
| `javax.inject.*`            | `jakarta.inject.*`            |
| `javax.validation.*`        | `jakarta.validation.*`        |
| `javax.ws.rs.*`             | `jakarta.ws.rs.*`             |

## REST API Endpoint Changes

The following REST API changes have been made.

| Endpoint                | Description              |
|-------------------------|--------------------------|
| `GET /example/resource` | TODO placeholder comment |
