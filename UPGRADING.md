Upgrading to Graylog 7.3.x
==========================

## Breaking Changes

### Beats compressed and decompressed packet sizes are now limited by default

Previously very large Beats packets with high compression ratios could lead to memory exhaustion in the server.
Beats inputs now use 16 MB for compressed packets and 64 MB after uncompressing them to limit this effect.
These values are configurable per input to allow for large packets when required.

See [Limit beats packet compressed and decompressed sizes- #26141](https://github.com/Graylog2/graylog2-server/pull/26141) for details.
