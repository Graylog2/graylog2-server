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

## Behaviour Changes

- The `system_messages` collection in MongoDB will be created as a 50MB capped collection going forward.
  This happens at creation, so existing `system_messages` collections remain unconstrained.
<br>You can manually convert your existing collection to a capped collection by following 
these [instructions](https://www.mongodb.com/docs/manual/core/capped-collections/#convert-a-collection-to-capped).
