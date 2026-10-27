Upgrading to Graylog 7.3.x
==========================

## `stream_aware_field_types` Is Now Enabled by Default

The `stream_aware_field_types` option now defaults to `true`. Graylog then tracks which fields are used in which streams,
so field lists and field types match the streams you are searching in. We enabled it because more and more features rely on accurate, stream-specific field types.

If your `graylog.conf` still contains `stream_aware_field_types=false` from a previous version, the option stays disabled.
If you don't configure it (e.g. Docker setups using only environment variables), it will be enabled after the upgrade.

Enabling this option can decrease performance on systems with many streams and fields. To disable it, set
`stream_aware_field_types = false` (or `GRAYLOG_STREAM_AWARE_FIELD_TYPES=false`).
