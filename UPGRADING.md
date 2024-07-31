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

## New Configuration Settings

`license_manager_url` specifies the base path of the new License Manager component.

## Java API Changes

The following Java Code API changes have been made.

| File/method                         | Description            |
|-------------------------------------|------------------------|
| `BlockingBatchedESOutput`           | The class got removed. |
| `GreyNoiseCommunityIpLookupAdapter` | The class got removed. |

## REST API Endpoint Changes

The following REST API changes have been made.

| Endpoint                                 | Description                                                                                                                     |
|------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| `PUT /system/indices/index_set_defaults` | This endpoint now expects an index set template id as payload. The values of the index set template are used as default values. |
| `GET licenses/{licenseId}`          | deprecated                                                                             |
| `GET licenses`                      | deprecated                                                                             |
| `GET licenses/status`               | deprecated                                                                             |
| `GET licenses/status/active`        | New: Show status for currently active license                                          |
| `GET licenses/validity/for-subject` | Check for valid license for given subject                                              |
| `GET licenses/status/for-subject`   | deprecated                                                                             |
| `DELETE licenses/{licenseId}`        | When called with a contract ID it will delete the contract and all associated licenses |
| `GET licenses/traffic-remaining` | Get the time series data for remaining provisioned traffic                             |
| `GET licenses/metrics` | Get the stats for consumed and remaining provisioned traffic                                                                                       |

## Deprecated Inputs

The following enterprise Google inputs have been deprecated. Also, a new enterprise Google Workspace has been introduced, 
which supports retrieving many types of Workspace logs, including the logs from the deprecated inputs. Log parsing for 
the new Workspace input is expected to be delivered in a future Graylog Illuminate version.

- Gmail Log Events
- Google Workspace Log Events
