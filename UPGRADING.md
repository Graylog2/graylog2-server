Upgrading to Graylog 8.0.x
==========================

## Breaking Changes

### Slack and Microsoft Teams Notifications Enforce the URL Allowlist

Starting with Graylog 8.0, Slack and Microsoft Teams event notifications now enforce the URL allowlist
(System > Configurations > URL Allowlist). If you already have Slack or Teams notifications configured, 
their webhook URLs need to be added to the URL Allowlist before upgrading so they continue working. 

Installations that have the allowlist disabled entirely are unaffected by this change.

To find the notifications that need to be added to the Allowlist, you can run this query against your 
Graylog MongoDB database. It returns the title, type, and webhook URL of every configured Slack and Teams notification:

```
db.event_notifications.find(
  { "config.type": { $in: ["slack-notification-v1", "teams-notification-v1", "teams-notification-v2"] } },
  { _id: 0, title: 1, "config.type": 1, "config.webhook_url": 1 }
)
```

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

### Changed parsing for Okta Log Events `securityContext.userBehaviors` field

Due to a [bug in the Okta SDK](https://github.com/okta/okta-sdk-java/issues/1689), a workaround was introduced in 7.1
to stringify the objects in the `securityContext.userBehaviors` array in logs pulled in from the `Okta Log Events` input. The
updated SDK now properly serializes that field as an array of objects and the workaround has been removed. Custom parsing
on `Okta Log Events` messages that is expecting the `securityContext.userBehaviors` field to be an array of
strings will need to be modified to expect an array of objects, per the Okta API. An example of the serialization across
versions:

7.1:
```json
{
  "securityContext": {
    "userBehaviors": [
      "{\"name\":\"New City\",\"id\":\"bbbbbbbbbbbbbbbbbbbb\",\"result\":\"NEGATIVE\"}",
      "{\"name\":\"New Country\",\"id\":\"aaaaaaaaaaaaaaaaaaaa\",\"result\":\"NEGATIVE\"}"
    ]
  }
}
```

7.2:
```json
{
  "securityContext": {
    "userBehaviors": [
      {
        "name": "New City",
        "id": "bbbbbbbbbbbbbbbbbbbb",
        "result": "NEGATIVE"
      },
      {
        "name": "New Country",
        "id": "aaaaaaaaaaaaaaaaaaaa",
        "result": "NEGATIVE"
      }
    ]
  }
}
```
### Scripting API default fields on message export
Per default, we now export all fields in a message on export. Prior to this change, we defaulted to a limited list of 
fields but had no option to export all fields. So a user would have to know (via the FE) which fields actually exist. 
Now you can export with all fields and limit the results by specifying the fields wanted.

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

## Plugin Builds: New `requireUpperBoundDeps` Maven Enforcer Rule

Plugin builds inheriting from the `graylog-plugin-parent` or `graylog-plugin-web-parent` Maven parent
POM now run the
[`requireUpperBoundDeps`](https://maven.apache.org/enforcer/enforcer-rules/requireUpperBoundDeps.html)
enforcer rule. It fails the build when a transitive dependency resolves to a *lower* version than
another of the plugin's dependencies requires.

Such conflicts can usually be fixed by updating the outdated dependency, or by adding a
`<dependencyManagement>` entry for the flagged artifact using the highest required version shown in
the error message. Plugin authors who are unable to align their dependencies can override the
`enforce-versions` execution of the `maven-enforcer-plugin` in their own POM.

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

### Sigma Level to Event Definition Priority Correction

Event definitions created from Sigma rules previously mapped to only a select set of the supported priority values.
They now map to the full range, and all existing rules have the correct priority applied during the 7.2 upgrade.

| Sigma level     | Before     | After             |
| --------------- | ---------- | ----------------- |
| `informational` | 1 (Low)    | 0 (Informational) |
| `low`           | 1 (Low)    | 1 (Low)           |
| `medium`        | 2 (Medium) | 2 (Medium)        |
| `high`          | 3 (High)   | 3 (High)          |
| `critical`      | 3 (High)   | 4 (Critical)      |

### Changes to Sigma Events

- The `sigma_rule_tag_*` fields are no longer added to fired events. Previously, fired events included
  `sigma_rule_tag_1`, `sigma_rule_tag_2`, etc. in their Additional Fields. MITRE information recognized by Graylog is
  now stored on the Event Definition as `tactics_techniques`, and any other tag values are moved to a dedicated `tags`
  field. Both `tactics_techniques` and `tags` are multi-valued (array) fields, unlike the individual
  `sigma_rule_tag_N` fields they replace. If you rely on the `sigma_rule_tag_*` fields in summary templates,
  notification bodies, or downstream processing, you will need to update those references.
- The `Sigma: ` prefix is no longer added to fired event titles. Events already stored in the index keep their
  original title, but newly fired events will not include the prefix.

For previously imported Sigma rules, the upgrade migration applies these changes to the Event Definitions
automatically: each source Sigma rule's tag values are written onto its Event Definition (MITRE references into
`tactics_techniques`, all other tags into `tags`), and the `Sigma: ` prefix is removed from the Event Definition
title. You do not need to re-import or reconfigure your rules. Note that events already stored in the index are not
rewritten — they keep the original `sigma_rule_tag_*` Additional Fields and titles they were fired with; the changes
above apply to events fired after the upgrade.

### Sigma API Changes

| Endpoint                                                                       | Description                                                                              |
|--------------------------------------------------------------------------------|------------------------------------------------------------------------------------------|
| `POST /plugins/org.graylog.plugins.securityapp.sigma/sigma/rules/validate_zip` | Moved to `POST /plugins/org.graylog.plugins.securityapp.sigma/sigma/import/validate_zip` |
| `POST /plugins/org.graylog.plugins.securityapp.sigma/sigma/rules/import`       | Moved to `POST /plugins/org.graylog.plugins.securityapp.sigma/sigma/import/bulk/import`  |
| `POST /plugins/org.graylog.plugins.securityapp.sigma/sigma/rules/upload`       | Moved to `POST /plugins/org.graylog.plugins.securityapp.sigma/sigma/import/bulk/upload`  |
| All other `/plugins/org.graylog.plugins.securityapp.sigma/sigma/rules/...`     | Deleted (use Event Definition API to manage)                                             |

## Threat Coverage Percentages May Change After Upgrade

Due to the migration of Sigma rules to Event Definitions in 7.2, the percentages displayed in the Threat
Coverage widget may be different from what they were in 7.1. Coverage is now computed directly from Event
Definitions rather than from the previous Sigma rules. Every Event Definition with MITRE tactics/techniques
assigned is now included, and coverage reflects how many of them are enabled versus disabled (with no log
source check that was previously present for Sigma rules). Therefore, a tactic may show a higher or lower
percentage than it did in 7.1, without any change to the actual installed Event Definitions.

## AWS Kinesis/CloudWatch Input: Required DynamoDB Permissions

In Graylog 7.2, the AWS Kinesis/CloudWatch input has been upgraded to Kinesis Client Library (KCL) 3.5, which calls
DynamoDB actions that KCL 2.x did not. **This applies to every Kinesis/CloudWatch input, whether it is new or existed
before 7.2**, so a policy written for an earlier Graylog release can look correct and still deny the input. A policy
that grants only the actions KCL 2.x needed leaves the input logging an authorization error on a fixed schedule while
consuming no records. In 7.2 Graylog watches the two calls the consumer cannot work without, DynamoDB lease discovery
(`Query`) and the Kinesis record fetch (`GetRecords`), and fails the input naming the denied action once one has been
denied continuously for two minutes; a denial KCL works around, such as one affecting only lease rebalancing, still
leaves the input running.

In addition to the actions the lease table already needed (`CreateTable`, `DescribeTable`, `GetItem`, `PutItem`,
`Scan`, `UpdateItem`, `DeleteItem`), KCL 3.5 requires:

- **`dynamodb:Query` on `arn:aws:dynamodb:<region>:<account>:table/graylog-aws-plugin-*/index/*`.** KCL 3.5 discovers
  leases through a global secondary index on the lease table. An index is a separate IAM resource from its table, so
  granting `Query` on the table alone still denies this call.
- **`dynamodb:UpdateTable` on `arn:aws:dynamodb:<region>:<account>:table/graylog-aws-plugin-*`.** KCL creates that
  index on first start. If this is denied, KCL retries the call throughout startup and then aborts, so the input fails
  with a generic initialization error rather than starting. That is outside the steady-state detection described above,
  which watches denials of `Query` and `GetRecords` on a running input, but the input still fails visibly instead of
  consuming nothing.

The two legacy tables, `<application-name>-CoordinatorState` and `<application-name>-WorkerMetricStats`, also need
access, and how much depends on the input:

- **An input created on 7.2 or later** needs only `dynamodb:DescribeTable` on both. KCL checks whether they exist on
  every start and treats anything other than "table not found" as a failure, so a policy scoped to the lease table
  alone prevents the input from starting even though it never had these tables.
- **An input that existed before 7.2** keeps using both tables until you complete the single-table migration described
  in the next section. Until then they need the same item-level actions as the lease table (`GetItem`, `PutItem`,
  `UpdateItem`, `DeleteItem`, `Scan`) in addition to `DescribeTable`: KCL holds its leader lock in
  `-CoordinatorState` and writes worker metrics to `-WorkerMetricStats` every 30 seconds.

A policy whose resource is `arn:aws:dynamodb:<region>:<account>:table/graylog-aws-plugin-*` covers the lease table and
both legacy tables, because their names share that prefix.

If you enable the single-table migration described in the next section, it moves state entities in a DynamoDB
transaction, which additionally requires `dynamodb:ConditionCheckItem` on the `-CoordinatorState` table alongside
the item-level actions above. `ConditionCheckItem` is used only by transactions, so policies written for
non-transactional access usually omit it, and without it the migration never completes while reporting nothing in
the Graylog UI.

## AWS Kinesis/CloudWatch Input: Single DynamoDB Table State Tracking

The KCL stores its coordination state in DynamoDB. Before KCL 3.5, this used three tables per input: the lease table, plus separate
`<application-name>-CoordinatorState` and `<application-name>-WorkerMetricStats` tables. KCL 3.5 introduced a
single-table format that consolidates all of this into the lease table alone (each item is tagged with an
`entityType` attribute). This reduces the number of DynamoDB tables and helps you stay under account-level table
limits.

There are two things to know about how this applies to your inputs:

- **New inputs use the single-table format automatically.** This is the AWS KCL 3.5 default for a newly created
  consumer. A fresh input always uses a single lease table.
- **Existing inputs keep their three-table layout** until you deliberately migrate them using the new
    "Migrate to single DynamoDB table for state tracking" input option.  

To let you migrate existing inputs on your own schedule, the input's **edit page** exposes a 
**Migrate to single DynamoDB table for state tracking** option. This option is only relevant for inputs created before Graylog 7.2.
Enabling it on such an input starts a one-way migration
that consolidates the `-CoordinatorState` and `-WorkerMetricStats` entities into the input's lease table. Stream
checkpoints are preserved, so ingestion should continue without replay or gaps. 

### Migration steps for a Kinesis input that existed before Graylog 7.2

1. Upgrade to Graylog 7.2 and allow the input to start and run for one hour with the **Migrate to single DynamoDB
   table for state tracking** option off. KCL 3.5 requires the input to run steadily for a whole hour before it will
   accept the migration. This readiness period is fixed by KCL 3.5 and cannot be shortened.
2. Before enabling the option, confirm the input is ready. In DynamoDB, open the input's coordinator-state table
   (`graylog-aws-plugin-<stream-name>-CoordinatorState`), find the `TableMigration3.5` item, and check its `tm`
   attribute. It must read `TABLE_MIGRATION_STATUS_DEPLOYED`. If it still reads `TABLE_MIGRATION_STATUS_INIT`, the
   readiness period has not elapsed yet; wait and re-check. Enabling the option before this point causes the input
   to fail to start.
3. Enable the **Migrate to single DynamoDB table for state tracking** option on the input's edit page and save. The
   migration begins.
4. Verify completion. KCL 3.5 bakes for 24 hours (the default) before finalizing. After that period, check the same
   `TableMigration3.5` item again; its `tm` attribute should read `TABLE_MIGRATION_STATUS_COMPLETE`. Once complete,
   the legacy `-CoordinatorState` and `-WorkerMetricStats` tables are no longer used and can be deleted.

The migration is **one-way and cannot be reverted once complete.** It is also not instantaneous, and AWS recommends
monitoring the migration until it reaches completion.

For how to monitor the migration, along with the migration steps, required permissions, and how to remove the
now-unused legacy tables afterward, see AWS's documentation:

- [Single table format for KCL](https://docs.aws.amazon.com/streams/latest/dev/kcl-single-table-format.html)
- [Migrate from KCL 2.x to KCL 3.x](https://docs.aws.amazon.com/streams/latest/dev/kcl-migration-from-2-3.html)
