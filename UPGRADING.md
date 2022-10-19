Upgrading to Graylog 5.0.x
==========================

:::(Warning) Warning
Please make sure to create a MongoDB database backup before starting the upgrade to Graylog 5.0!
:::

## Breaking Changes

* Graylog 5 is Java 17 only. We no longer support earlier versions starting with 5.0
* Support for Elasticsearch 6.X has been removed! Please use either Elasticsearch 7.10.2 or, preferably, latest OpenSearch.

## Disallowing embedding the frontend by default

To prevent [click-jacking](https://developer.mozilla.org/en-US/docs/Web/Security/Types_of_attacks#click-jacking), we are now preventing the frontend to be embedded in `<frame>`/`<iframe>`/etc. elements by sending the `X-Frame-Options`-header with all HTTP responses. The header value depends on the new configuration setting `http_allow_embedding`. The different combinations are:

| `http_allow_embedding` | `X-Frame-Options`-header value |
|------------------------|--------------------------------|
| not set                | `DENY`                         |
| `false`                | `DENY`                         |
| `true`                 | `SAMEORIGIN`                   |

If you want to be able to embed the Graylog frontend in another HTML page, you most probably want to set `http_allow_embedding` to `true`. Only do this if you are aware of the implications!

For further information about the meanings of the different header values and how they are interpreted by browsers, please read [this](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Frame-Options).

## New Default Message Processing Order

The new default Message Processing order will run the
`Message Filter Chain` before the `Pipeline Processor`.

This applies only to new Graylog installations.
Existing setups keep the former default order for backwards compatibility.

## API Endpoint Deprecations

The following API endpoints are deprecated beginning with 5.0.

| Endpoint                                    | Description                 |
| ------------------------------------------- | --------------------------- |
| `PUT /example/placeholder`                  | TODO placeholder comment    |

## API Endpoint Removals

The following API endpoints have been removed in 5.0.

### Legacy Alert API
5.0 eliminates all of the previously deprecated legacy alert APIs. 
Content packs that include legacy alerts can still be installed, but the alerts will be silently ignored.

| Endpoint                                                        | Description                         |
|-----------------------------------------------------------------|-------------------------------------|
| `GET /alerts/conditions`                                        | Removed deprecated legacy alert API |
| `GET /alerts/conditions/types`                                  | Removed deprecated legacy alert API |
| `GET /streams/{streamId}/alerts/conditions`                     | Removed deprecated legacy alert API |
| `POST /streams/{streamId}/alerts/conditions`                    | Removed deprecated legacy alert API |
| `GET /streams/{streamId}/alerts/conditions/{conditionId}`       | Removed deprecated legacy alert API |    
| `PUT /streams/{streamId}/alerts/conditions/{conditionId}`       | Removed deprecated legacy alert API |    
| `DELETE /streams/{streamId}/alerts/conditions/{conditionId}`    | Removed deprecated legacy alert API |    
| `POST /streams/{streamId}/alerts/conditions/test`               | Removed deprecated legacy alert API |    
| `POST /streams/{streamId}/alerts/conditions/{conditionId}/test` | Removed deprecated legacy alert API |    
| `GET /streams/{streamId}/alerts`                                | Removed deprecated legacy alert API |
| `GET /streams/{streamId}/alerts/paginated`                      | Removed deprecated legacy alert API |
| `GET /streams/{streamId}/alerts/check`                          | Removed deprecated legacy alert API |
| `POST /streams/{streamId}/alerts/receivers`                     | Removed deprecated legacy alert API |
| `DELETE /streams/{streamId}/alerts/receivers`                   | Removed deprecated legacy alert API |
| `POST /streams/{streamId}/alerts/sendDummyAlert`                | Removed deprecated legacy alert API |


| Endpoint                                    | Description                 |
| ------------------------------------------- | --------------------------- |
| `GET /system/metrics/{metricName}/history`  | Remove unused and dysfunctional endpoint. (part of [#2443](https://github.com/Graylog2/graylog2-server/pull/2443)) |


## API Endpoint Changes

| Endpoint                                         | Description                                                                                                                                                                       |
|--------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `POST & PUT /system/inputs/{inputId}/extractors` | Renamed request body parameter `cut_or_copy` to `cursor_strategy` & changed type of request body parameter `converters` to List<Map<String, Object>> as returned in the GET calls |                                                                                                

## Java Code API Deprecations

The following Java Code API deprecations have been made in 5.0.

- The `org.graylog2.plugin.PluginModule.addNotificationType(name, notificationClass, handlerClass, factoryClass)`
  method has been deprecated in favor of a new/preferred version, which also properly registers the notification 
  config content pack entity, so that instances the corresponding content pack entity can can be installed successfully:
`org.graylog2.plugin.PluginModule.addNotificationType(name, notificationClass, handlerClass, factoryClass, contentPackEntityName, contentPackEntityClass)`. 
  See <PR link> for more info.

## Java Code API Changes

The following Java Code API changes have been made in 5.0.

| File                                                                                                   | Description                                              |
|--------------------------------------------------------------------------------------------------------|----------------------------------------------------------|
| `PaginatedPipelineService.java` | Concrete implementation has been changed to an interface |
| `PaginatedRuleService.java`     | Concrete implementation has been changed to an interface |

## Configuration File Changes

| Option                                        | Action       | Description                                                     |
|-----------------------------------------------|--------------|-----------------------------------------------------------------|
| `mongodb_threads_allowed_to_block_multiplier` | **removed**  | Configuring this is not supported by the official MongoDB driver anymore. |

## Behaviour Changes

- The Prometheus metrics for Graylog inputs were previously only exposed for
  inputs of type `GELFHttpInput`. They are now exposed for all configured inputs
  and labeled accordingly. To support this, the default prometheus mappings for
  the following metrics have been changed: 
  - `input_empty_messages`
  - `input_incoming_messages`
  - `input_open_connections`
  - `input_raw_size`
  - `input_read_bytes_one_sec`
  - `input_read_bytes_total`
  - `input_total_connections`
  - `input_written_bytes_one_sec`
  - `input_written_bytes_total`
- The `system_messages` collection in MongoDB will be created as a 50MB capped collection going forward.
  This happens at creation, so existing `system_messages` collections remain unconstrained.
<br>You can manually convert your existing collection to a capped collection by following 
these [instructions](https://www.mongodb.com/docs/manual/core/capped-collections/#convert-a-collection-to-capped).
- Introducing new archive config parameter `retentionTime` in days. 
  Archives exceeding the specified retention time are automatically deleted. 
  By default the behavior is unchanged: archives are retained indefinitely. 
- Introducing new input config option `encoding`, enabling users to override the default
UTF-8 encoding. 
<br>Note that this encoding is applied to all messages received by the input. A single input
cannot handle multiple log sources with different encodings.
- The permissions for which options are populated in the System dropdown menu were updated to more closely match the page that they link to. See [graylog2-server#13188](https://github.com/Graylog2/graylog2-server/pull/13188) for details.
The Page permissions remain unchanged but this could affect the workflow for users with legacy permissions.
- Newly created aggregation widgets will now have rollup disabled by default. Existing widgets are unchanged.

### Changed archived default path
On new Graylog installations, the default archiving configuration will now 
store archives under the `data_dir` instead of `/tmp/graylog-archives`. 
(The `data_dir` is configured in graylog.conf and defaults to `/var/lib/graylog-server`)

### Configuring archive retention Time and max value
It is now possible to configure default archive retention time and a limit via config flags
`default_archive_retention_time` & `max_archive_retention_time`.
