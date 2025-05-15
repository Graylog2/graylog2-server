Upgrading to Graylog 6.3.x
==========================

## Breaking Changes

- By introducing a report creator role in Enterprise, we change from the functionality in the 
previous version that allowed every user to create reports to a more restrictive approach that a 
user needs the report creator role from now on to create reports.

## Configuration File Changes

| Option        | Action     | Description                                    |
|---------------|------------|------------------------------------------------|
| `tbd`         | **added**  |                                                |

## Default Configuration Changes

- A permission `input_types:create` for creating input types has been introduced.

  By granting only permissions for specific input types (e.g.
  `input_types:create:org.graylog2.inputs.misc.jsonpath.JsonPathInput`),
  users can be only allowed to manage inputs of specific types. Granting the permission without specifying input
  types (as shown above) will allow management of all input types.
  Existing roles and users are updated to automatically include the permissions for all input types if they contain a
  manage permission for inputs.

## Java API Changes

- tbd

## REST API Endpoint Changes

The following REST API changes have been made.

| Endpoint                                                              | Description                                                                             |
|-----------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
| `GET /<endpoint>`                                                     | description                                                                             |
