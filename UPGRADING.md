Upgrading to Graylog 5.2.x
==========================

## New Functionality

- New pipeline rule functions for manipulating maps: `map_set` and `map_remove`.

## Breaking Changes

## Deprecation and Change in Functionality of GreyNoise Data Adapters

- GreyNoise Community IP Lookup Data Adapters have been marked as deprecated. Existing Data Adapters can no longer be
  started or lookups performed.
- GreyNoise Full IP Lookup [Enterprise] Data Adapter can no longer be used with a free GreyNoise Community API tokens.
- GreyNoise Quick IP Lookup Data Adapter can no longer be used with a free GreyNoise Community API tokens.

## Shutdown of Graylog on OutOfMemoryError
Because of an error in HttpCore 4.4.12, which is required by Elasticsearch and older versions of Opensearch, OutOfMemoryError errors were not properly handled.
The Reactor was stopped, which prevented proper Graylog operation and the reason (OutOfMemoryError) was not clearly visible.
From now on, Graylog will shutdown on OutOfMemoryError, trying to log some basic information about the thread and memory consumption during this event.

## Log parsing changes in the CrowdStrike input

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

## Java API Changes
The following Java Code API changes have been made.

| File/method                   | Description                                                     |
|-------------------------------|-----------------------------------------------------------------|
| `ExampleClass#exampleFuntion` | TODO placeholder comment             |


## REST API Endpoint Changes
The following REST API changes have been made.

| Endpoint                                 | Description                                |
|------------------------------------------|--------------------------------------------|
| `GET /contentstream/settings/{username}` | Retrieve Content Stream settings for specified user |
| `PUT /contentstream/enable/{username}`   | Enable Content Stream for specified user   |
| `PUT /contentstream/disable/{username}`  | Disable Content Stream for specified user  |
| `PUT /contentstream/topics/{username}`   | Update per user Content Stream topic list  |



