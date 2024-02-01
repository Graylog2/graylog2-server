Upgrading to Graylog 5.2.x
==========================

## New Functionality

- New pipeline rule functions for manipulating maps: `map_set` and `map_remove`.

## Breaking Changes
- If you use the DataNode, the system clocks of the nodes have to be synchronized with an external source for JWT usage (within a margin of a couple of seconds).

### Migrating from legacy index templates to composable index templates

Starting with Graylog 5.2, we are migrating from using [legacy index templates](https://www.elastic.co/guide/en/elasticsearch/reference/7.17/indices-templates-v1.html) to [composable index template]
(https://www.elastic.co/guide/en/elasticsearch/reference/7.17/index-templates.html). While this gives us more flexibility and predictability for field types in index mappings, this also requires that existing custom legacy index templates need to be migrated to composable index templates manually as well.

### Removed support for legacy "Collector Sidecars"

Graylog 3.0 introduced "Graylog Sidecars" as a replacement for the old Collector Sidecars (version 0.1.x).

With Graylog 5.2, support for the legacy Collector Sidecars is finally removed.

Please refer to the migration guide [Upgrading from the Collector Sidecar](https://archivedocs.graylog.org/en/3.3/pages/sidecar.html#upgrading-from-the-collector-sidecar) if you are still using the old Sidecars before upgrading Graylog to 5.2.

## Deprecation and Change in Functionality of GreyNoise Data Adapters

- GreyNoise Community IP Lookup Data Adapters have been marked as deprecated. Existing Data Adapters can no longer be
  started or lookups performed.
- GreyNoise Full IP Lookup [Enterprise] Data Adapter can no longer be used with a free GreyNoise Community API tokens.
- GreyNoise Quick IP Lookup Data Adapter can no longer be used with a free GreyNoise Community API tokens.

## Shutdown of Graylog on OutOfMemoryError
Because of an error in HttpCore 4.4.12, which is required by Elasticsearch and older versions of Opensearch, OutOfMemoryError errors were not properly handled.
The Reactor was stopped, which prevented proper Graylog operation and the reason (OutOfMemoryError) was not clearly visible.
From now on, Graylog will shutdown on OutOfMemoryError, trying to log some basic information about the thread and memory consumption during this event.

## Configuration File Changes
| Option                           | Action    | Description                                                                                                                                                                                                                                             |
|----------------------------------|-----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `field_value_suggestion_mode`    | **added** | Allows controlling field value suggestions, turning them on, off, or allowing them only for textual fields.                                                                                                                                             |


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

## CrowdStrike input log parsing changes

Several log parsing changes have been made to the CrowdStrike input.

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

## Microsoft Defender for Endpoint input log parsing changes

Several log parsing changes have been made to the Microsoft Defender for Endpoint input.

Added fields:
`event_source_product`: Contains the static value `microsoft_defender_endpoint`.

Changed fields:
- `message`: Now contains the full message payload.
- `source`: Now contains the `detectionSource` log value.

Removed fields:
- `alert_signature`
- `alert_signature_id`
- `event_start`
- `event_end`
- `full_message`

Note that additional Microsoft Defender for Endpoint message parsing is expected to be released in a future release of
Graylog Illuminate.

## REST API Endpoint Changes
The following REST API changes have been made.

| Endpoint                                 | Description                                         |
|------------------------------------------|-----------------------------------------------------|
| `GET /contentstream/settings/{username}` | Retrieve Content Stream settings for specified user |
| `PUT /contentstream/enable/{username}`   | Enable Content Stream for specified user            |
| `PUT /contentstream/disable/{username}`  | Disable Content Stream for specified user           |
| `PUT /contentstream/topics/{username}`   | Update per user Content Stream topic list           |
| `GET /contentstream/tags`                | Retrieve Content Stream tags based on license       |



