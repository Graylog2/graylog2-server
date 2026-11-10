Upgrading to Graylog 7.3.x
==========================

## Breaking Changes

### `stream_aware_field_types` Is Now Enabled by Default

The `stream_aware_field_types` option now defaults to `true`. Graylog then tracks which fields are used in which streams,
so field lists and field types match the streams you are searching in. We enabled it because more and more features rely on accurate, stream-specific field types.

If your `graylog.conf` still contains `stream_aware_field_types=false` from a previous version, the option stays disabled.
If you don't configure it (e.g. Docker setups using only environment variables), it will be enabled after the upgrade.

Enabling this option can decrease performance on systems with many streams and fields. To disable it, set
`stream_aware_field_types = false` (or `GRAYLOG_STREAM_AWARE_FIELD_TYPES=false`).

### Microsoft Defender Vulnerabilities Now Counted per Affected Application

Assets imported from Microsoft Defender now store one vulnerability per CVE and affected application, instead of
one per CVE. A CVE that affects several applications on the same machine (for example, Application A and Application B) is now
counted once for each application, so the count more accurately reflects the number of vulnerable applications that
need to be patched. This is consistent with how vulnerabilities from other scanner sources, such as CrowdStrike, are
counted.

After the first Defender scanner sync following the upgrade, vulnerability counts may be higher than before in the
following locations where they are displayed:

- The Vulnerabilities column on the Assets page
- The Vulnerabilities tab in the asset details
- The `vulnerability_count_*` fields of messages in the Asset History stream

## Configuration File Changes

| Option | Action    | Description |
|--------|-----------|-------------|
| `tbd`  | **added** |             |

## Java API Changes

This release includes the following Java API changes.

| File/method                                              | Description                                                       |
|----------------------------------------------------------|-------------------------------------------------------------------|
| `org.graylog2.plugin.database.users.User#setPermissions` | Deprecated. Will be removed in Graylog 8.0. Assign roles instead. |

## REST API Endpoint Changes

This release includes the following REST API changes.

| Endpoint                               | Description                                                                   |
|----------------------------------------|-------------------------------------------------------------------------------|
| `POST /users`                          | The `permissions` field is deprecated and now optional. Assign roles instead. |
| `PUT /users/{userId}`                  | The `permissions` field is deprecated. Assign roles instead.                  |
| `PUT /users/{username}/permissions`    | Deprecated. Will be removed in Graylog 8.0.                                   |
| `DELETE /users/{username}/permissions` | Deprecated. Will be removed in Graylog 8.0.                                   |

## Deprecation of Direct User Permissions

Besides roles and shares, users can hold permissions directly. Starting with Graylog 7.3, direct user permissions are
deprecated. Support for them will be removed in Graylog 8.0.

The web interface doesn't manage direct user permissions, so this mainly affects REST API clients. Graylog logs a
warning when a request sets direct permissions or uses one of the deprecated endpoints listed above. To avoid the
warnings, omit the `permissions` field in `POST /users` and `PUT /users/{userId}`, and assign roles instead.
