Upgrading to Graylog 7.2.x
==========================

## Breaking Changes

### Entity Suggestion Search: Regex No Longer Supported in Query Parameter

The `query` parameter of the entity suggestion endpoint is now treated as a plain-text substring
rather than a regular expression. Previously, passing a regex pattern (e.g. `foo.*bar`) would be
evaluated by MongoDB, which allowed ReDoS attacks via crafted inputs.

After upgrading, queries containing regex metacharacters (`.`, `*`, `+`, `?`, `(`, `)`, etc.) will
be matched literally instead of being interpreted as a pattern.

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

## Web Interface Changes

### Event Definition "Fields" step renamed to "Additional Details"

The "Fields" step on the Event Definition wizard has been renamed to "Additional Details" to better reflect its
content, which now covers more than event fields (e.g. tags). Along with the visible label, the step's `step` query
parameter changed from `fields` to `additional-details`, so the URL for that step is now
`.../edit?step=additional-details`. Existing bookmarked links using the old `?step=fields` value will continue to
work, since they are mapped to the renamed step. You should update them to the new value, because support for the
old value may be removed in a future version.


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

## Threat Coverage Percentages May Change After Upgrade

Due to the migration of Sigma rules to Event Definitions in 7.2, the percentages displayed in the Threat
Coverage widget may be different from what they were in 7.1. Coverage is now computed directly from Event
Definitions rather than from the previous Sigma rules. Every Event Definition with MITRE tactics/techniques
assigned is now included, and coverage reflects how many of them are enabled versus disabled (with no log
source check that was previously present for Sigma rules). Therefore, a tactic may show a higher or lower
percentage than it did in 7.1, without any change to the actual installed Event Definitions.
