Upgrading to Graylog 4.4.x
==========================

:::(Warning) Warning
Please make sure to create a MongoDB database backup before starting the upgrade to Graylog 4.4!
:::

## Breaking Changes

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
