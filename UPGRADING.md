Upgrading to Graylog 6.3.x
==========================

## Breaking Changes

- tbd

## Configuration File Changes

| Option        | Action     | Description                                    |
|---------------|------------|------------------------------------------------|
| `tbd`         | **added**  |                                                |

## Default Configuration Changes

- A permission set for input types has been introduced.
  This includes the permissions `input_of_types:create`, `input_of_types:read`, `input_of_types:terminate`,
  `input_of_types:edit` and `input_of_types:changestate`.

  By granting only permissions for specific input types (e.g.
  `input_of_types:read:org.graylog2.inputs.misc.jsonpath.JsonPathInput`),
  users can be only allowed to see/manage inputs of specific types. Granting the permission without specifying input
  types (as shown above) will allow management of all input types.
  Existing roles are updated to automatically include the permissions for all input types if they contain the respective
  permission for inputs.

## Java API Changes

- tbd

## REST API Endpoint Changes

The following REST API changes have been made.

| Endpoint                                                              | Description                                                                             |
|-----------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
| `GET /<endpoint>`                                                     | description                                                                             |
