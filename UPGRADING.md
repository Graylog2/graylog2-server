Upgrading to Graylog 5.0.x
==========================

:::(Warning) Warning
Please make sure to create a MongoDB database backup before starting the upgrade to Graylog 5.0!
:::

## Breaking Changes

* Graylog 5 is Java 17 only. We no longer support earlier Java versions.
* Support for Elasticsearch 6.X has been removed! Please use either Elasticsearch 7.10.2 or, preferably, latest OpenSearch.
* Graylog 5 needs at least MongoDB 5.0. Our recommended upgrade path is to first bring your MongoDB to 5.0 and then perform the Graylog upgrade.
  Hint: Graylog 4.3.x does support MongoDB 5.0, which allows for a seamless upgrade path.
* The `flatten_json` pipeline function now preserves the original type of the extracted values, instead
of converting them to string. An optional flag is provided so existing rules can
continue using the legacy behavior.


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

## Stream aware field types

So far, when listing fields for a query, Graylog has been showing fields for all streams.
For some systems, this list may be extremely long and contain many fields which are not present in the query results.
It is now possible to change this behavior. When configuration property `stream_aware_field_types` is set to true, Graylog will periodically collect information on stream-field relation from Elasticsearch/Opensearch and use it to provide only those fields which are present in the streams used in the query.

If all of your streams go to dedicated, separate index sets, it is advised to keep the default value of `stream_aware_field_types` property (`false`). It will decrease the load on ES/OS and stream separation across index sets already helps with showing proper fields for a query.
On the other hand, if multiple streams go to the same index sets, and you want precise field types and suggestions, you should set it to `true`. Consider monitoring your ES/OS load after that change, especially when using huge numbers of fields and streams. 

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

| Option                                         | Action       | Description                                                               |
|------------------------------------------------|--------------|---------------------------------------------------------------------------|
| `mongodb_threads_allowed_to_block_multiplier`  | **removed**  | Configuring this is not supported by the official MongoDB driver anymore. |
| `outputbuffer_processor_threads_max_pool_size` | **removed**  | This setting has been removed because it was not effective.               |
| `outputbuffer_processor_keep_alive_time`       | **removed**  | This setting has been removed because it was not effective.               |

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
- The `JSON path value from HTTP API` input will now only run on the leader node,
  if the `Global` option has been selected in the input configuration.
  Previously, the input was started on all nodes in the cluster.

### Changed archived default path
On new Graylog installations, the default archiving configuration will now 
store archives under the `data_dir` instead of `/tmp/graylog-archives`. 
(The `data_dir` is configured in graylog.conf and defaults to `/var/lib/graylog-server`)

### Configuring archive retention Time and max value
It is now possible to configure default archive retention time and a limit via config flags:
`default_archive_retention_time` & `max_archive_retention_time` using a duration in days. e.g. 365d.

## Microsoft Teams Notification Template Changes
Microsoft Teams notification template parsing no longer parses each line in the template and tries to form a key-value
pair using a colon delimiter. This will result in Teams notifications with a templated custom message lacking any
formatting. Existing custom templates should be updated to use HTML or Markdown in order to display properly. If using
the old default template, it can be replaced with the one found when creating a new Teams notification. It can also be
found in this [pull request](https://github.com/Graylog2/graylog-plugin-integrations/pull/1202).

## Operating Systems

Graylog 5.0 removes official support for the following Linux distributions:

- Debian 8, 9
- Ubuntu 16.04, 18.04
- RHEL/CentOS 6

## Operating System Packages

### JVM Dependency

The Graylog 5.0 operating system packages bundle version 17 of the JVM, so it's no longer required to install any
JVM/Java packages to run Graylog. You can configure Graylog to use an external JVM if required.

### Removed Packages

The following operating system packages are no longer available in Graylog 5.0.

- `graylog-integrations-plugins`
- `graylog-enterprise-plugins`
- `graylog-enterprise-integrations-plugins`

Use the `graylog-enterprise` or `graylog-server` package instead.

### RPM Digest Changes

The RPM packages switched from the legacy `SHA1` digest to `SHA256` for package signatures.
The checksums of the files inside the package switched from the legacy `MD5` to `SHA256`.

### Package Upgrade

#### RPM

Install the latest [graylog-5.0-repository RPM package](https://downloads.graylog.org/repo/packages/graylog-5.0-repository_latest.rpm) to update the repository metadata.

**When using Graylog Operations**

```
sudo rpm -Uvh https://downloads.graylog.org/repo/packages/graylog-5.0-repository_latest.rpm
sudo yum clean all
sudo yum install graylog-enterprise
```

**When using Graylog Open**

```
sudo rpm -Uvh https://downloads.graylog.org/repo/packages/graylog-5.0-repository_latest.rpm
sudo yum clean all
sudo yum install graylog-server
```

If you are using the plugin packages, you have to remove them before upgrading.

#### DEB

Install the latest [graylog-5.0-repository DEB package](https://downloads.graylog.org/repo/packages/graylog-5.0-repository_latest.deb) to update the repository metadata.

**When using Graylog Operations**

```
wget https://downloads.graylog.org/repo/packages/graylog-5.0-repository_latest.deb
sudo dpkg -i graylog-5.0-repository_latest.deb
sudo apt-get update
sudo apt-get install graylog-enterprise
```

**When using Graylog Open**

```
wget https://downloads.graylog.org/repo/packages/graylog-5.0-repository_latest.deb
sudo dpkg -i graylog-5.0-repository_latest.deb
sudo apt-get update
sudo apt-get install graylog-server
```

# New Functionality

## Sidecar

The Sidecar Administration UI now allows the assignment of multiple configurations
for a single collector.
Please note that this feature requires a Sidecar with version 1.3 or greater.
Older versions will only run a single (random) configuration per collector.
