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

## Web Interface Changes

### Event Definition "Fields" step renamed to "Additional Details"

The "Fields" step on the Event Definition wizard has been renamed to "Additional Details" to better reflect its
content, which now covers more than event fields (e.g. tags). Along with the visible label, the step's `step` query
parameter changed from `fields` to `additional-details`, so the URL for that step is now
`.../edit?step=additional-details`. Existing bookmarked links should be updated to the new value, since the old 
`?step=fields` value is no longer valid.

## Java API Changes

| File/method                                                               | Description |
|---------------------------------------------------------------------------|-------------|
| `org.graylog2.contentpacks.facades.EntityWithExcerptFacade#resolveGrants` | removed     |

