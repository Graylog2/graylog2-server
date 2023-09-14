Upgrading to Graylog 5.2.x
==========================

## New Functionality

## Breaking Changes
- Default value for `data_dir` configuration option has been removed and must be specified in `graylog.conf`

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

| File/method                   | Description                                                      |
|-------------------------------|------------------------------------------------------------------|
| `ExampleClass#exampleFuntion` | TODO placeholder comment                                         |


## REST API Endpoint Changes
The following REST API changes have been made.

| Endpoint                                              | Description                               |
|-------------------------------------------------------|-------------------------------------------|
| `PUT /example/placeholder`                            | TODO placeholder comment                  |
