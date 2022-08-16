Upgrading to Graylog 4.4.x
==========================

:::(Warning) Warning
Please make sure to create a MongoDB database backup before starting the upgrade to Graylog 4.4!
:::

## Breaking Changes

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

## Disallow Disk Journal on Network Filesystems

On new Graylog installations, the disk journal directory cannot be placed
on network filesystems such as NFS, CIFS, etc anymore.
Existing installations will just receive a warning about this.


## API Endpoint Deprecations

The following API endpoints are deprecated beginning with 4.4.

| Endpoint                                    | Description                 |
| ------------------------------------------- | --------------------------- |
| `PUT /example/placeholder`                  | TODO placeholder comment    |

## API Endpoint Removals

The following API endpoints have been removed in 4.4.

| Endpoint                                    | Description                 |
| ------------------------------------------- | --------------------------- |
| `PUT /example/placeholder`                  | TODO placeholder comment    |

## Java Code API Changes

The following Java Code API changes have been made in 4.4.

| File                                                                                                   | Description                                              |
|--------------------------------------------------------------------------------------------------------|----------------------------------------------------------|
| `PaginatedPipelineService.java` | Concrete implementation has been changed to an interface |
| `PaginatedRuleService.java`     | Concrete implementation has been changed to an interface |

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

### Changed archived default path
On new Graylog installations, the default archiving configuration will now 
store archives under the `data_dir` instead of `/tmp/graylog-archives`. 
(The `data_dir` is configured in graylog.conf and defaults to `/var/lib/graylog-server`)
