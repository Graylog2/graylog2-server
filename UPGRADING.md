Upgrading to Graylog 7.1.x
==========================

## User Session Termination

All user sessions will be terminated when upgrading because the internal storage format for sessions has been changed.
Users will have to log in again.

## Breaking Changes

### External Authentication Services: Changed Default User Time Zone

The authentication backends for Active Directory, LDAP, OIDC, Okta, and SAML previously set the time zone for
newly synchronized users to the value of the `root_timezone` config file setting. ("UTC" by default)

Graylog 7.1 introduces a configurable "default user time zone" setting for all authentication backends.
The default value is unset, meaning that the browser's time zone will be used by default.

## Configuration File Changes

| Option | Action    | Description |
|--------|-----------|-------------|
| `tbd`  | **added** |             |

## Java API Changes

- tbd

## REST API Endpoint Changes

The following REST API changes have been made.

| Endpoint             | Description                        |
|----------------------|------------------------------------|
| `GET /<endpoint>`    | Description of the endpoint change |
