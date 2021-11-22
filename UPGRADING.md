Upgrading to Graylog 4.3.x
==========================

:::(Warning) Warning
Please make sure to create a MongoDB database backup before starting the upgrade to Graylog 4.3!
:::

## Breaking Changes

## API Endpoint Deprecations

The following API endpoints are deprecated beginning with 4.3.

| Endpoint                                    | Description                 |
| ------------------------------------------- | --------------------------- |
| `PUT /example/placeholder`                  | TODO placeholder comment    |

## API Endpoint Removals

The following API endpoints have been removed in 4.3.

| Endpoint                                    | Description                 |
| ------------------------------------------- | --------------------------- |
| `PUT /example/placeholder`                  | TODO placeholder comment    |

## Removed Migrations

Removed two migrations that convert pre-1.2 user permissions and index ranges to newer formats.

## Configuration File Changes

The following configuration option as been removed: `index_field_type_periodical_interval`.

It has been replaced with a new configuration option which allows users to tweak the default full refresh interval for
index field type information: `index_field_type_periodical_full_refresh_interval`.
