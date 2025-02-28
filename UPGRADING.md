Upgrading to Graylog 6.1.x
==========================

## Breaking Changes

- API errors which are related to invalid requests (e.g. JSON parsing failures, invalid types, etc.) are now responded to with a type of `RequestError` instead of `APIError`.
- Removed GreyNoise Community Data Adapter deprecated in 5.2.

## Default Configuration Changes

- The `default_message_output_class` setting changed from
`org.graylog2.outputs.BlockingBatchedESOutput` to 
`org.graylog2.outputs.BatchedMessageFilterOutput` as part of an internal
refactoring. Regular users should not change the setting.

- The `output_batch_size` setting can now be configured by providing a byte-based value, e.g. `10 mb`. For backward
compatibility, the default value is still count-based (`500`). Previously configured count-based values are
still supported.

- The legacy mode configuration setting of newly created Kafka based inputs has been changed to false. The default mode
will now use the high level consumer API that has been available since Kafka 1.x.

## Java API Changes

The following Java Code API changes have been made.

| File/method                         | Description            |
|-------------------------------------|------------------------|
| `BlockingBatchedESOutput`           | The class got removed. |
| `GreyNoiseCommunityIpLookupAdapter` | The class got removed. |

## REST API Endpoint Changes

The following REST API changes have been made.

| Endpoint                                                              | Description                                                                                                                     |
|-----------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| `PUT /system/indices/index_set_defaults`                              | This endpoint now expects an index set template id as payload. The values of the index set template are used as default values. |
| `GET licenses/{licenseId}`                                            | deprecated                                                                                                                      |
| `GET licenses`                                                        | deprecated                                                                                                                      |
| `GET licenses/status`                                                 | deprecated                                                                                                                      |
| `GET licenses/status/active`                                          | New: Show status for currently active license                                                                                   |
| `GET licenses/validity/for-subject`                                   | Check for valid license for given subject                                                                                       |
| `GET licenses/status/for-subject`                                     | deprecated                                                                                                                      |
| `DELETE licenses/{licenseId}`                                         | When called with a contract ID it will delete the contract and all associated licenses                                          |
| `GET licenses/traffic-remaining`                                      | Get the time series data for remaining provisioned traffic                                                                      |
| `GET licenses/metrics`                                                | Get the stats for consumed and remaining provisioned traffic                                                                    |
| `GET licenses/traffic-threshold`                                      | Get info about license traffic threshold warning                                                                                |
| `PUT licenses/traffic-threshold/acknowledgement`                      | Acknowledge current traffic threshold warning                                                                                   |
| `DELETE /datatiering/repositories/index_sets/{id}/failed_snapshot`    | Delete any FAILED or PARTIAL snapshots for this index set                                                                       |
| `GET /plugins/org.graylog.integrations/aws/inputs/available_services` | Remove unused endpoint.                                                                                                         |
| `GET /plugins/org.graylog.integrations/aws/inputs/permissions`        | Removed permissions endpoint in favor of maintaining permissions in official docs site.                                         | 

## Deprecated Inputs

The following enterprise Google inputs have been deprecated. Also, a new enterprise Google Workspace has been introduced, 
which supports retrieving many types of Workspace logs, including the logs from the deprecated inputs. Log parsing for 
the new Workspace input is expected to be delivered in a future Graylog Illuminate version.

- Gmail Log Events
- Google Workspace Log Events
