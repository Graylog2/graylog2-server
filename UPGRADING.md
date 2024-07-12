Upgrading to Graylog 6.1.x
==========================

## Breaking Changes

- API errors which are related to invalid requests (e.g. JSON parsing failures, invalid types, etc.) are now responded to with a type of `RequestError` instead of `APIError`.

## Default Configuration Changes

The `default_message_output_class` setting changed from
`org.graylog2.outputs.BlockingBatchedESOutput` to 
org.graylog2.outputs.BatchedMessageFilterOutput` as part of an internal
refactoring. Regular users should not change the setting.

## Java API Changes

The following Java Code API changes have been made.

| File/method               | Description                                     |
|---------------------------|-------------------------------------------------|
| `BlockingBatchedESOutput` | The class got removed.                          |

## REST API Endpoint Changes

The following REST API changes have been made.

| Endpoint                 | Description                                      |
|--------------------------|--------------------------------------------------|
| `tbd`                    | tbd                                              |
