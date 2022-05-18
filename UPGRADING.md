Upgrading to Graylog 4.3.x
==========================

:::(Warning) Warning
Please make sure to create a MongoDB database backup before starting the upgrade to Graylog 4.3!
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

### Notable API Endpoint Changes ##

| Endpoint                        | Description                 |
| ------------------------------- | --------------------------- |
| `GET /api/system/configuration` | The field `stale_master_timeout` has been deprecated. It is still present in the response object for backwards compatibility but will hold the same value as the new `stale_leader_timeout` field, which has been added as a replacement. |

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

## Java Code API Deprecations

The following Java Code API deprecations have been made in 4.3.

- The `org.graylog2.plugin.PluginModule.addNotificationType(name, notificationClass, handlerClass, factoryClass)`
  method has been deprecated in favor of a new/preferred version, which also properly registers the notification 
  config content pack entity, so that instances the corresponding content pack entity can can be installed successfully:
`org.graylog2.plugin.PluginModule.addNotificationType(name, notificationClass, handlerClass, factoryClass, contentPackEntityName, contentPackEntityClass)`. 
  See <PR link> for more info.

## Removed Migrations

- Removed two migrations that convert pre-1.2 user permissions and index ranges to newer formats.
- Removed a migration that was recalculating the index ranges of the default stream's index set for
pre 2.2 Graylog installations.

## Configuration File Changes

| Option                                              | Action      | Description |
| --------------------------------------------------- | ----------  | ------------ |
| `is_master`                                         | **removed** | Replaced with `is_leader`. For backwards compatibility, `is_master` will still be evaluated, but `is_leader` takes precedence, if both are configured.|
| `stale_master_timeout`                              | **removed** | Replaced with `stale_leader_timeout`. For backwards compatibility, `stale_master_timeout` will still be evaluated, but `stale_leader_timeout` takes precedence, if both are configured. |
| `index_field_type_periodical_interval`              | **removed** | To control index field type refreshing, the new `index_field_type_periodical_full_refresh_interval` may be used instead. |
| `is_leader`                                         | *added*     | Replacement for `is_master` to promote [inclusive naming](https://inclusivenaming.org/faqs/). |
| `stale_leader_timeout`                              | *added*     | Replacement for `stale_master_timeout` to promote [inclusive naming](https://inclusivenaming.org/faqs/). |
| `index_field_type_periodical_full_refresh_interval` | *added*     | Allows users to tweak the default interval after which field type information will be refreshed for *all* indices. |

## Behaviour Changes

- Pipeline function `drop_message` was modified to provide more performant and 
predictable results: When `drop_message` is called in a rule, we complete processing of
the current stage; following stages are skipped. Other pipelines operating on the same message will
complete stage numbers less than or equal to the aborting stage; higher numbered stages are skipped.  
- Restarting the server would restart all inputs, even those that were manually stopped.
We now persist the desired input state: manually stopped inputs remain stopped, even after a server restart.
<br>Users can choose the legacy behaviour by setting a flag in the `graylog.conf`:  
  `auto_restart_inputs = true`

- Introducing new archive config parameter retentionTime in days. 
  Archives exceeding the specified retentionTime are automatically deleted. 
  By default the behavior is unchanged: archives are retained indefinitely. 
