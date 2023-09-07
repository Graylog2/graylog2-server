Upgrading to Graylog 5.2.x
==========================

## New Functionality

- New pipeline rule functions for manipulating maps: `map_set` and `map_remove`.

## Breaking Changes

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



