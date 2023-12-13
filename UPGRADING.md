Upgrading to Graylog 5.1.x
==========================

## New Functionality

### Index Default Configuration
Support for configuring index defaults has been added:
1) Adds the ability to specify Index Set initialization default settings in the server configuration file for new Graylog clusters.
2) Adds the ability to subsequently maintain the current Index Set defaults from the System > Configurations page
   and through the Graylog API.

#### New Graylog Cluster Index Set Initialization Defaults
New Graylog server clusters can now initialize the settings for Index Sets with the following server configuration
values. Please see the sample [graylog.conf](https://github.com/Graylog2/graylog2-server/blob/master/misc/graylog.conf) file for more details and example values.

- `elasticsearch_analyzer`
- `elasticsearch_shards`
- `elasticsearch_replicas`
- `disable_index_optimization`
- `index_optimization_max_num_segments`
- `rotation_strategy`
- `elasticsearch_max_docs_per_index`
- `elasticsearch_max_size_per_index`
- `elasticsearch_max_time_per_index`
- `retention_strategy`
- `elasticsearch_max_number_of_indices`

If you are using a pre-existing version of the `graylog.conf` configuration file, it is recommended that you review the
aforementioned settings there before upgrading, to ensure the in-database defaults are established as expected with the
upgrade. Although the `graylog.conf` sample configuration file now ships with all index default example
properties commented out, you may be using an older version of the file where certain index default values were present
and not commented-out.

All previously deprecated index set configuration properties in `org.graylog2.configuration.ElasticsearchConfiguration`
have been un-deprecated, as Graylog intends to maintain them going forward.

Once the first Graylog server instance is started to establish the cluster, the following system indexes will be created
with the specified defaults.

- Default index set
- Graylog Events
- Graylog System Events
- Graylog Message Failures
- Restored Archives

#### In-database Cluster Index Set Defaults

The current in-database defaults for new Index Sets can now be edited from the new System > Configuration >
Index Set defaults configuration area. The default values set here will be used for all new index sets created:

- Those created from the System > Index Sets page.
- New indexes created through the Graylog Illuminate system.

Once the upgrade is installed, these in-database defaults will be established, and the server configuration option
values described above will no longer be used.

The in-database Index Set default configuration can also be edited VIA the Graylog API:

```
curl 'http://graylog-server:8080/api/system/indices/index_set_defaults' \
  -X 'PUT' \
  -H 'Content-Type: application/json' \
  -H 'X-Requested-By: my-api-request' \
  --data-raw '
  {
    "index_analyzer": "standard",
    "shards": 1,
    "replicas": 0,
    "index_optimization_max_num_segments": 1,
    "index_optimization_disabled": false,
    "field_type_refresh_interval": 300,
    "field_type_refresh_interval_unit": "SECONDS",
    "rotation_strategy_class": "org.graylog2.indexer.rotation.strategies.SizeBasedRotationStrategy",
    "rotation_strategy_config": {
      "type": "org.graylog2.indexer.rotation.strategies.SizeBasedRotationStrategyConfig",
      "max_size": 32212254720
    },
    "retention_strategy_class": "org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy",
    "retention_strategy_config": {
      "type": "org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig",
      "max_number_of_indices": 20
    }
  }'
```

#### New Index Default values
Unless user-specified defaults are specified, the following new defaults will be effective for all new index sets created:

- Shards: 1 (previously 4 in many cases)
- Rotation Strategy: Time Size Optimizing - 30-40 days (previously Index Time [1D] in many cases)

#### Import Custom Sigma Rule Repositories
Previously, the official Sigma HQ rule repository was the only repository that could be used to import Sigma rules. Now
any public Git repository containing Sigma rule source files can be imported to Graylog and rules can be imported from
them. Since Graylog no longer reads directly from the Sigma HQ repository, it must be imported before new rules can be
added from it. To expedite this process, on the `Security > Sigma Rules > Sigma Repos` page there is an `Add SigmaHQ`
button that will import the repository. The repository is about 10MB and all Sigma rule source files will be copied into
MongoDB so the clone may take a minute to complete. Once the repository has been added rules can be imported as they were
in 5.0.

The rules within imported repositories must conform to the
[Sigma specification](https://github.com/SigmaHQ/sigma-specification/blob/main/Sigma_specification.md) in order to be
successfully added. Since the repositories are stored locally they will not have the latest changes automatically applied
but can easily be refreshed in the `Sigma Repos` tab using the `Refresh` menu item for each repository.

## Removal of deprecated Inputs

The following inputs are no longer available:
- AWS Logs (deprecated)
- AWS Flow Logs (deprecated)

The inputs were marked as deprecated since Graylog version `3.2.0`.
If you still run any of those inputs, please configure the alternative "Kinesis/CloudWatch" input instead ahead of upgrading.

## CrowdStrike input log parsing changes

Several log parsing changes have been made to the CrowdStrike input in Graylog release 5.1.6.

Added fields:
`event_created`: Contains the `metadata.eventCreationTime` log value.
`vendor_subtype`: Contains the `metadata.eventType` log value.
`vendor_version`: Contains the `metadata.version` log value.
`event_source_product`: Contains the static value `crowdstrike_falcon`.

Changed fields:
- `message`: Now contains the JSON content of the log `event` value, effectively the message payload.
- The message `timestamp` field is now set to the current Graylog system date/time, instead of the previously used log `metadata.eventCreationTime` value.

Removed fields:
- `event_end`
- `event_source`
- `event_start`
- `user_domain`
- `user_id`
- `vendor_event_description`
- `FILE_NAME`
- `FILE_PATH`
- `OBJECTIVE`
- `TECHNIQUE`

Note that additional CrowdStrike message parsing is expected to be released in a future release of Graylog Illuminate.

## Input log parsing changes

Log parsing changes have been made several inputs in preparation for Illuminate parsing content. Note that additional 
message parsing for these inputs is expected to be released in an upcoming release of Graylog Illuminate.

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

## Java API Changes
The following Java Code API changes have been made.

| File/method                                  | Description                                                                                                 |
|----------------------------------------------|-------------------------------------------------------------------------------------------------------------|
| `IndexSetValidator#validateRefreshInterval`  | The method argument have changed from `IndexSetConfig` to `Duration`                                        |
| `IndexSetValidator#validateRetentionPeriod`  | The method argument have changed from `IndexSetConfig` to `RotationStrategyConfig, RetentionStrategyConfig` |
| `ElasticsearchConfiguration#getIndexPrefix`  | The method name has changed to `getDefaultIndexPrefix`                                                      |
| `ElasticsearchConfiguration#getTemplateName` | The method name has changed to `getDefaultIndexTemplateName`                                                |
| `AuthServiceBackendConfig#externalHTTPHosts` | This method was added to the interface                                                                      |


All previously deprecated index set configuration properties in `org.graylog2.configuration.ElasticsearchConfiguration`
have been un-deprecated, as Graylog intends to maintain them going forward. 

## REST API Endpoint Changes

| Endpoint                                                                                                   | Description                                                                                                                                      |
|------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| `GET /system/configuration`                                                                                | Key `gc_warning_threshold` has been removed from response object.                                                                                |                                                                                                
| `PUT /plugins/org.graylog.plugins.forwarder/forwarder/profiles/{inputProfileId}/inputs/{forwarderInputId}` | Added `type` as a required request attribute.                                                                                                    |
| `GET /streams/paginated`                                                                                   | Key `streams` in response has been changed to `elements`. Attribute `pagination` has been added to response to bundle all pagination attributes. |
| `GET /dashboards`                                                                                          | Key `views` in response has been changed to `elements`. Attribute `pagination` has been added to response to bundle all pagination attributes.   |
| `GET /search/saved`                                                                                        | Key `views` in response has been changed to `elements`. Attribute `pagination` has been added to response to bundle all pagination attributes.   |
| `GET /events/definitions`                                                                                  | Endpoint has been deprecated in favor of `/events/definitions/paginated` (see below)                                                             |
| `GET /events/definitions/paginated`                                                                        | Returns a paginated list of event definitions with request parameters and response format suited for the new entity data table                   |
| `GET /events/notifications`                                                                                | Endpoint has been deprecated in favor of `/events/notifications/paginated` (see below)                                                           |
| `GET /events/notifications/paginated`                                                                      | Returns a paginated list of event notifications with request parameters and response format suited for the new entity data table                 |

### Change to the format of `Input` entities in API responses

This change applies to the format of input entities in responses to the resources at or beneath `/system/inputs` and `plugins/org.graylog.plugins.forwarder/forwarder/profiles`.

Input configuration may now contain values of type [EncryptedValue](https://github.com/Graylog2/graylog2-server/blob/f35df42e165ac570b8b27de3f8eeac85e74ed610/graylog2-server/src/main/java/org/graylog2/security/encryption/EncryptedValue.java).
Sensitive input configuration values for various inputs may be stored encrypted from now on and will therefore be represented differently in the JSON response.

For example, an input previously rendered like this in a response:
```json
{
  "id": "63f489ee73561d699b210677",
  "attributes": {
    "not_so_secret_value": "plaintext",
    "secret_value": "plaintext",
    ...
  },
  ...
}
```

will be returned like this if the `secret` attribute contains a sensitive value:

```json
{
  "id": "63f489ee73561d699b210677",
  "attributes": {
    "not_so_secret_value": "plaintext",
    "secret_value": {
      "is_set": true
    },
    ...
  },
  ...
}
```

### Added Optional Default Timezone configuration for Syslog inputs
When creating or editing a new syslog input, it is now possible to configure a default timezone in case logs ingested are not
sending dates in UTC. When left as "Not configured", system behaves as before. 

The following REST API endpoints were changed:

| Endpoint                                                                                                   | Description                                   |
|------------------------------------------------------------------------------------------------------------|-----------------------------------------------|
| `PUT /plugins/org.graylog.plugins.forwarder/forwarder/profiles/{inputProfileId}/inputs/{forwarderInputId}` | Added `type` as a required request attribute. |

## Behaviour Changes

- The `JSON path value from HTTP API` input will now only run on the leader node, if the `Global` option has been selected in the input configuration. Previously, the input was started on all nodes in the cluster.
- The default connection and read timeouts for email sending have been reduced from 60 seconds to 10 seconds.
- We are now parsing the time zone information send by Fortigate syslog messages. Any workarounds to transform the date into the right timezone because the forwared timezone information was not honored, should be removed.

## Configuration File Changes

| Option                                      | Action  | Description                                                                                     |
|---------------------------------------------|---------|-------------------------------------------------------------------------------------------------|
| `gc_warning_threshold`                      | removed | GC warnings have been removed.                                                                  |
| `transport_email_socket_connection_timeout` | added   | Connection timeout for establishing a connection to the email server. Default: 10 seconds.      |
| `transport_email_socket_timeout`            | added   | Read timeout while communicating with the email server. Default: 10 seconds.                    |
| `disabled_retention_strategies`             | added   | Allow disabling of `none` `close` `delete` retention strategies. At least one must stay enabled |
