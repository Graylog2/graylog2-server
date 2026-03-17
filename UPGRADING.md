Upgrading to Graylog 7.1.x
==========================

## User Session Termination

All user sessions will be terminated when upgrading because the internal storage format for sessions has been changed.
Users will have to log in again.

## Breaking Changes

### Plugins: Removal of Perspective Plugin API

This release includes frontend plugin API changes related to perspectives.

- The `perspectives` plugin export has been removed from core.
- Plugin-provided `navigation` and `pageNavigation` entities no longer support the `perspective` key.

### Security Events UI Routes Consolidated into Alerts

The dedicated Security Events UI routes under `/security/security-events/*` have been removed in favor of the
existing Alerts routes.

If you have bookmarks, links, or runbooks pointing to old routes, update them as follows:

| Old route                                               | New route                             |
| ------------------------------------------------------- | ------------------------------------- |
| `/security/security-events/alerts`                      | `/alerts`                             |
| `/security/security-events/definitions`                 | `/alerts/definitions`                 |
| `/security/security-events/notifications`               | `/alerts/notifications`               |
| `/security/security-events/event-procedures/procedures` | `/alerts/event-procedures/procedures` |
| `/security/security-events/event-procedures/steps`      | `/alerts/event-procedures/steps`      |
| `/security/security-events/event-procedure-action`      | `/alerts/event-procedure-action`      |

### External Authentication Services: Changed Default User Time Zone

The authentication backends for Active Directory, LDAP, OIDC, Okta, and SAML previously set the time zone for
newly synchronized users to the value of the `root_timezone` config file setting. ("UTC" by default)

Graylog 7.1 introduces a configurable "default user time zone" setting for all authentication backends.
The default value is unset, meaning that the browser's time zone will be used by default.

### Formatting Change of `aggregation_conditions` Field in Aggregation Events

The `aggregation_conditions` map previously used keys with parentheses on the aggregation type. These needed to be
escaped if they were used directly in Notification templates, e.g. `${aggregation_conditions.count\\(\\)}`,
`${aggregation_conditions.sum\\(fieldname\\)}`. To avoid the need for escaping, their format has been modified to use
underscores instead, e.g. `${aggregation_conditions.count}`, `${aggregation_conditions.sum_fieldname}`. Any
existing notifications using the escaping of parentheses in explicit `aggregation_conditions` key names will need to
be modified to instead use the underscore format.

### OpenSearch-Based Anomaly Detection Removed

Anomaly detection now runs natively within Graylog, removing the dependency on OpenSearch's Anomaly Detection plugin. 
This provides a more integrated experience with Alerts and Events, and does not require OpenSearch-specific
configuration.

As part of this change, existing OpenSearch-based anomaly detectors are no longer supported and will be
automatically disabled during the upgrade to Graylog 7.1. A software migration will stop and remove all
OpenSearch anomaly detectors and delete their associated event definitions.

After the upgrade, previously configured detectors will remain visible in the Anomaly Detection Configuration page 
for reference, showing their name and whether they were previously enabled. However, the full detector
configuration (indices, feature fields, intervals, etc.) will not be displayed and detectors can no longer be
edited or re-enabled. **Note: If you have custom anomaly detectors, you should note down their configuration 
before upgrading.**

## Configuration File Changes

| Option | Action    | Description |
| ------ | --------- | ----------- |
| `tbd`  | **added** |             |

## Java API Changes

- tbd

## REST API Endpoint Changes

The following REST API changes have been made.

| Endpoint          | Description                        |
| ----------------- | ---------------------------------- |
| `GET /<endpoint>` | Description of the endpoint change |
