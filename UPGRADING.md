Upgrading to Graylog 6.1.x
==========================

## Breaking Changes

- API errors which are related to invalid requests (e.g. JSON parsing failures, invalid types, etc.) are now responded to with a type of `RequestError` instead of `APIError`.
- Removed GreyNoise Community Data Adapter deprecated in 5.2.

## Default Configuration Changes

The `default_message_output_class` setting changed from
`org.graylog2.outputs.BlockingBatchedESOutput` to 
org.graylog2.outputs.BatchedMessageFilterOutput` as part of an internal
refactoring. Regular users should not change the setting.

## Java API Changes

The following Java Code API changes have been made.

| File/method                         | Description            |
|-------------------------------------|------------------------|
| `BlockingBatchedESOutput`           | The class got removed. |
| `GreyNoiseCommunityIpLookupAdapter` | The class got removed. |

## REST API Endpoint Changes

The following REST API changes have been made.

| Endpoint                 | Description                                      |
|--------------------------|--------------------------------------------------|
| `tbd`                    | tbd                                              |

## Deprecated Inputs

The following enterprise Google inputs have been deprecated. Also, a new enterprise Google Workspace has been introduced, 
which supports retrieving many types of Workspace logs, including the logs from the deprecated inputs. Log parsing for 
the new Workspace input is expected to be delivered in a future Graylog Illuminate version.

- Gmail Log Events
- Google Workspace Log Events
