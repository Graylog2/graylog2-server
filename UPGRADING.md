Upgrading to Graylog 7.0.x
==========================

## Breaking Changes

### Java 21

Graylog now requires Java 21 to run. Earlier versions are no longer supported.

Our operating system packages and container images are shipping with the
correct Java version.

### Mongo DB 7.0

Graylog now requires at least Mongo DB version 7.0. Earlier versions are no longer supported.

In general, MongoDB upgrades must be done from one minor release to the next, going to the latest bug fix version 
in that release. Please refer to the Mongo DB upgrade documentation for details:
- [Upgrade tutorial](https://www.mongodb.com/docs/manual/tutorial/upgrade-revision/#std-label-upgrade-to-latest-revision/)
- [6.0](https://www.mongodb.com/docs/manual/release-notes/6.0-upgrade/)
- [7.0](https://www.mongodb.com/docs/manual/release-notes/7.0-upgrade/)
- [8.0](https://www.mongodb.com/docs/manual/release-notes/8.0-upgrade/)

### Kafka Inputs

The `kafka-clients` library was updated to 4.x which removes support for Kafka
brokers with version 2.0 and earlier. That means all Graylog 7.0 Kafka inputs
can only talk to Kafka brokers with version 2.1 or newer.

### Enterprise Theme Color Customization

The logic for generating color shades based on custom-defined color variants (error, informative, etc.)
has been slightly adjusted. This change ensures that the exact color specified in the customization settings
is now used as the primary color for elements like buttons and badges in the UI.

### Renaming "Data Warehouse" to "Data Lake"
The feature previously known as "Data Warehouse" is now completely renamed to "Data Lake". That includes not only text
visible to the user, but with version 7.0 also a lot of places being usually invisible to the regular user, such as API
endpoints, the content of the database, permissions and much more.

#### API Endpoints and permissions
All endpoints related to data lake have changed their URLs accordingly. As also noted in
the [REST API endpoint Changes](#rest-api-endpoint-changes), all endpoints previously accessible at
`/api/plugins/org.graylog.plugins.datawarehouse/data_warehouse/...` are now moved to
`/api/plugins/org.graylog.plugins.datalake/data_lake/...`. Similarly, all permissions regarding data lake are renamed
from `data_warehouse...` to `data_lake...`.

#### Database content
Three entire collections are renamed: 
- `data_warehouse_archive_config` to `data_lake_archive_config`
- `data_warehouse_backends` to `data_lake_backends`
- `data_warehouse_catalog` to `data_lake_catalog`

Also, documents of the following collections are updated to reflect the change in names:
- `cluster_config` for migrations related to data lake
- `enterprise_traffic`
- `scheduler_job_definitions`
- `scheduler_triggers`, in case a data lake optimization job is already scheduled.

#### Audit logs
Audit logs having been written before the update are not changed. However, all audit logs from after the update contain
the term "Data Lake" instead of "Data Warehouse".

#### Metrics
Just like audit logs, metrics from before the update to version 7.0 are not changed. Starting with version 7.0, the
names of data lake related metrics change accordingly.


## Configuration File Changes

| Option | Action    | Description |
|--------|-----------|-------------|
| `tbd`  | **added** |             |

## Default Configuration Changes

- The permission to view the "Cluster Configuration" page was removed from the `Reader` role. This permission is now
  available with the `Cluster Configuration Reader` role. There is an automatic one-time migration to add this role to
  all existing users with the `Reader` role to ensure backwards compatibility. New users that will be created in the
  future need to be explicitly assigned to the `Cluster Configuration Reader` role if they should be able to access the
  page.
- Only admins are allowed to create a new API token. Existing tokens are not affected by this change. Also, new tokens
  will expire after 30 days by default.

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
    - Content Pack installation
  - Teams

  <br> For example, the request payload to create a stream might now look like this:

```json
{
  "entity": {
    "index_set_id": "65b7ba138cdb8c534a953fef",
    "description": "An example stream",
    "title": "My Stream",
    "remove_matches_from_default_stream": false
  },
  "share_request": {
    "selected_grantee_capabilities": {
      "grn::::search:684158906442150b2eefb78c": "own"
    }
  }
}
```

- Access to the API browser now requires the `api_browser:read` permission. This permission can be granted by assigning
  the new “API Browser Reader” role to a user.

## REST API Endpoint Changes

The following REST API changes have been made.

| Endpoint                                                                    | Description                                                                                                                                        |
|-----------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| `GET /system/urlallowlist`                                                  | Renamed from `GET /system/urlwhitelist`. The corresponding REST API permission is renamed to `urlallowlist:read`.                                  |
| `PUT /system/urlallowlist`                                                  | Renamed from `PUT /system/urlwhitelist`. The corresponding REST API permission is renamed to `urlallowlist:write`                                  |
| `POST /system/urlallowlist/check`                                           | Renamed from `POST /system/urlwhitelist/check`                                                                                                     |
| `POST /system/urlallowlist/generate_regex`                                  | Renamed from `POST /system/urlwhitelist/generate_regex`                                                                                            |
| All `/api/plugins/org.graylog.plugins.datalake/data_lake/...`               | Renamed from `/api/plugins/org.graylog.plugins.datawarehouse/data_warehouse/...`. The corresponding permissions are also renamed to `data_lake...` |
| All `/api/plugins/org.graylog.plugins.securityapp.asset/assets/history/...` | Removed all endpoints and contents of underlying `asset_history` MongoDB collection migrated to `Asset History` Index set and Stream               |
| `GET /<endpoint>`                                                           | description                                                                                                                                        |
