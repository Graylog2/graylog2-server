Upgrading to Graylog 7.0.x
==========================

## Breaking Changes

### Kafka Inputs

The `kafka-clients` library was updated to 4.x which removes support for Kafka
brokers with version 2.0 and earlier. That means all Graylog 7.0 Kafka inputs
can only talk to Kafka brokers with version 2.1 or newer.

## Configuration File Changes

| Option        | Action     | Description                                    |
|---------------|------------|------------------------------------------------|
| `tbd`         | **added**  |                                                |

## Default Configuration Changes

- The permission to view the "Cluster Configuration" page was removed from the `Reader` role. This permission is now
  available with the `Cluster Configuration Reader` role. There is an automatic one-time migration to add this role to
  all existing users with the `Reader` role to ensure backwards compatibility. New users that will be created in the
  future need to be explicitly assigned to the `Cluster Configuration Reader` role if they should be able to access the
  page.

## Java API Changes

- tbd

## General REST API Changes

- In Graylog 7.0, an issue was fixed that previously allowed additional unknown JSON properties to be accepted 
  (and ignored) in API requests on the Graylog leader node. Now that the issue has been fixed, API requests on the 
  leader node will once again only accept JSON payloads that contain explicitly mapped/supported properties.
- APIs for entity creation now use a parameter `CreateEntityRequest` to keep entity fields separated from sharing 
  information. This is a breaking change for all API requests that create entities, such as streams, dashboards, etc.
  <br> Affected entities: 
  - Search / Dashboard 
  - Search Filter 
  - Report
  - Event Definition
  - Stream
  - Notifications
  - Sigma rules
  - Event procedure
  - Event step
  
  <br> For example, the request payload to create a stream might now look like this:

```json
{
    "entity":{
        "index_set_id":"65b7ba138cdb8c534a953fef",
        "description":"An example stream",
        "title":"My Stream",
        "remove_matches_from_default_stream":false
    },
    "share_request":{
        "selected_grantee_capabilities":{
            "grn::::search:684158906442150b2eefb78c":"own"
        }
    }
}
```
- Access to the API browser now requires the `api_browser:read` permission. This permission can be granted by assigning 
  the new “API Browser Reader” role to a user.

## REST API Endpoint Changes

The following REST API changes have been made.

| Endpoint                                                              | Description                                                                             |
|-----------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
| `GET /<endpoint>`                                                     | description                                                                             |
| `GET /<endpoint>`                                                     | description                                                                             |
