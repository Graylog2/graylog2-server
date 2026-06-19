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

### Removed deprecated License REST endpoints

Four deprecated License REST endpoints (under `/api/plugins/org.graylog.plugins.license/licenses`)
that serialized the internal `LicenseStatus` model directly have been removed. They were marked
`@Deprecated(forRemoval = true)` and were no longer used by the Graylog web interface:

- `GET /licenses`
- `GET /licenses/{licenseId}`
- `GET /licenses/status`
- `GET /licenses/status/for-subject`

API clients still calling these will now receive a `404`. Use the following endpoints instead:

- `GET /licenses/status/active` for the active license status.
- `GET /licenses/status/paginated` for a paginated list of license statuses.
- `GET /licenses/validity/for-subject` to check license validity for a subject.

## Java API Changes

| File/method                                                               | Description |
|---------------------------------------------------------------------------|-------------|
| `org.graylog2.contentpacks.facades.EntityWithExcerptFacade#resolveGrants` | removed     |

