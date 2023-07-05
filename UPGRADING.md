Upgrading to Graylog 5.2.x
==========================

## New Functionality

## Breaking Changes

## Deprecation and Change in Functionality of GreyNoise Data Adapters

- GreyNoise Community IP Lookup Data Adapters have been marked as deprecated. Existing Data Adapters can no longer be
  started or lookups performed.
- GreyNoise Full IP Lookup [Enterprise] Data Adapter can no longer be used with a free GreyNoise Community API tokens.
- GreyNoise Quick IP Lookup Data Adapter can no longer be used with a free GreyNoise Community API tokens.

## Java API Changes
The following Java Code API changes have been made.

| File/method                   | Description                                                     |
|-------------------------------|-----------------------------------------------------------------|
| `ExampleClass#exampleFuntion` | TODO placeholder comment             |


## REST API Endpoint Changes
The following REST API changes have been made.

| Endpoint                              | Description                                |
|---------------------------------------|--------------------------------------------|
| `GET /contentStream/settings/{username}` | Retrieve Content Stream settings for specified user |
| `PUT /contentStream/enable/{username}` | Enable Content Stream for specified user   |
| `PUT /contentStream/disable/{username}` | Disable Content Stream for specified user  |
| `PUT /contentStream/topics/{username}` | Update per user Content Stream topic list  |



