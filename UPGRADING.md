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

## Java API Changes

| File/method                                                               | Description |
|---------------------------------------------------------------------------|-------------|
| `org.graylog2.contentpacks.facades.EntityWithExcerptFacade#resolveGrants` | removed     |

## Sigma Rules Folded into Event Definitions

Prior to 7.2, Sigma rules were first order entities that could be managed directly. Each rule was also
backed by an Event Definition that controlled the execution scheduling and could also have some of its
configuration managed directly. Sigma rules have now been folded into Event Definitions and the first
order Sigma rule entity no longer exists. Sigma rule event definitions can now be created by either a
file upload or configured Git repository import. The manual modification of Sigma rule source YAML is
no longer supported. After a Sigma rule has been imported as an Event Definition, all management is now
handled directly on the Event Definition. In support of this change, the `Security > Sigma Rules` menu option and its
associated UI has been removed. Management of Sigma rule Git repositories has moved to `Alerts > Sigma Repos`.

Sigma Correlation rules can no longer be directly imported or uploaded. For `event_count` and `value_count` types,
the correlated rules can be imported and then the resulting event definitions can be modified to add aggregation
information without needing to import another Sigma rule. For `temporal_ordered` types, an `Event Correlation` event
definition can be created after the rules are imported to create the same temporal correlation.

All previously imported Sigma rules, including correlation rules, will be migrated to the new Event Definition pattern
on upgrade and work as they did before.

The following REST API changes are a direct result of this rework:

| Endpoint                                                                       | Description                                                                              |
|--------------------------------------------------------------------------------|------------------------------------------------------------------------------------------|
| `POST /plugins/org.graylog.plugins.securityapp.sigma/sigma/rules/validate_zip` | Moved to `POST /plugins/org.graylog.plugins.securityapp.sigma/sigma/import/validate_zip` |
| `POST /plugins/org.graylog.plugins.securityapp.sigma/sigma/rules/import`       | Moved to `POST /plugins/org.graylog.plugins.securityapp.sigma/sigma/import/bulk/import`  |
| `POST /plugins/org.graylog.plugins.securityapp.sigma/sigma/rules/upload`       | Moved to `POST /plugins/org.graylog.plugins.securityapp.sigma/sigma/import/bulk/upload`  |
| All other `/plugins/org.graylog.plugins.securityapp.sigma/sigma/rules/...`     | Deleted                                                                                  |
