Upgrading to Graylog 7.3.x
==========================

## Microsoft Defender Vulnerabilities Now Counted per Affected Application

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

## `stream_aware_field_types` Is Now Enabled by Default

The `stream_aware_field_types` option now defaults to `true`. Graylog then tracks which fields are used in which streams,
so field lists and field types match the streams you are searching in. We enabled it because more and more features rely on accurate, stream-specific field types.

If your `graylog.conf` still contains `stream_aware_field_types=false` from a previous version, the option stays disabled.
If you don't configure it (e.g. Docker setups using only environment variables), it will be enabled after the upgrade.

Enabling this option can decrease performance on systems with many streams and fields. To disable it, set
`stream_aware_field_types = false` (or `GRAYLOG_STREAM_AWARE_FIELD_TYPES=false`).
