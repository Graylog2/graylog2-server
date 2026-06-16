Upgrading to Graylog 7.2.x
==========================

## Breaking Changes

### Paginated REST APIs: Case-Insensitive Matching and Sorting

Paginated entity endpoints (e.g. Streams, Event Definitions, Notifications, Lookup Tables, Dashboards,
Sigma Rules, Investigations, etc.) now share a case-insensitive, numeric-aware collation for both
**sorting** and **filtering**. Previously, only sort order was affected when individual endpoints
opted in; matching was always case-sensitive.

After upgrading:

- Sorting by string fields such as `title` or `name` interleaves upper- and lower-case entries,
  and strings containing numbers sort naturally (`Stream 2` before `Stream 10`).
- Filter expressions on string fields match case-insensitively. For example, a query that previously
  matched only `test` now also matches `Test` and `TEST`. API clients relying on exact-case matching
  via paginated endpoints will see additional results.

### `gl2_accounted_message_size` can now be `0` for restored Data Lake messages

When messages are restored from the Data Lake, those that do not count against your license traffic
now have their `gl2_accounted_message_size` field set to `0`. Previously the field always held the
message's accounted size, regardless of whether the restore counted against the license.

This field is informational and is not used to compute license usage, so your license consumption is
unaffected by the change.

## Java API Changes

| File/method                                                               | Description |
|---------------------------------------------------------------------------|-------------|
| `org.graylog2.contentpacks.facades.EntityWithExcerptFacade#resolveGrants` | removed     |

