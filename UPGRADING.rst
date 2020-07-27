**************************
Upgrading to Graylog 3.3.x
**************************

.. _upgrade-from-32-to-33:

[BREAKING] Fixing certificate validation for LDAP servers used for authentication
=================================================================================

Prior to v3.3.3, the certificates of LDAP servers which are connected to using a secure connection (SSL or TLS) were not validated, even if the "Allow self-signed certificates" option was unchecked. Starting with v3.3.3, certificates are validated against the local default keystore. This might introduce a breaking change, depending on your local LDAP settings and the validity of the certificates used (if any). Please ensure that all certificates used are valid, their common name matches the host part of your configured LDAP server and your local keystore contains all CA/intermediate certs required for validation.

A `CVE <https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2020-15813>`_ is tracked for this issue.

Deprecating legacy Aggregation API endpoints
============================================

This release is marking several endpoints of the legacy (pre 3.2) aggregation API as being deprecated. They will be removed in 4.0. These include:

- `/search/universal/(absolute|relative|keyword)/`
    - `terms-histogram`
    - `histogram`
    - `fieldhistogram`
    - `stats`
    - `termsstats`
    - `terms`
- `/sources`

These endpoints are not being used by the frontend anymore. In general, we try to replace very specific endpoints with more general, flexible ones.
Deprecating and removing these endpoints frees development time for new things, which would otherwise need to be invested in maintaining legacy code.
All of the functionality offered by these endpoints can be implemented by the `Views` API in a better way, please consult your local Swagger instance for details.


API Access Token Encryption
===========================

For improved security, all API access tokens will now be stored encrypted in the database. Existing API tokens will automatically be encrypted by a database migration on Graylog server startup.

.. warning:: The token encryption is using the ``password_secret`` value from ``graylog.conf`` (or ``/etc/graylog/server/server.conf``) as encryption key. All Graylog nodes in the cluster need to have the same value configured for that option to make sure encryption/decryption works correctly. (if the values differ across your nodes, use the one from the master node for all other nodes)

Dashboards API
==============

Since 3.2.0, the legacy dashboards API was still accessible and functional under `/dashboards`, you could create, manipulate and delete legacy dashboards, but this had no effect in the frontend.
Starting with 3.3.0, the legacy dashboards API will be moved to `/legacy/dashboards`. The current dashboards will be accessible through `/dashboards` again. The pre-3.2.0 route for the current dashboards (`/views/dashboards`) will redirect there as well.
Please note that the format has changed. You can see the new format for dashboards in the API browser.

We are planning to remove the legacy dashboards API and the `/views/dashboards` redirect in the next major upgrade of Graylog.

Saved Searches API
==================

Since 3.2.0, the legacy saved searches API was still accessible and functional under `/search/saved`, you could create, manipulate and delete legacy saved searches, but this had no effect in the frontend.
Starting with 3.3.0, the legacy saved searches API will be moved to `/legacy/search/saved`. The current saved searches will be accessible through `/search/saved` again. The pre-3.2.0 route for the current saved searches (`/views/savedSearches`) will redirect there as well.
Please note that the format has changed. You can see the new format for saved searches in the API browser.

We are planning to remove the legacy saved searches API and the `/views/savedSearches` redirect in the next major upgrade of Graylog.

CSV Export API
==============

For 3.3.0 a new endpoint for creating CSV exports has been added under `/views/search/messages`.

We are planning to remove the older export endpoints in the next major upgrade of Graylog:
- `/search/universal/absolute/export`
- `/search/universal/keyword/export`
- `/search/universal/relative/export`

Notes for plugin authors
========================

Prior to 3.2.0, it was possible to consume a special `OkHttpClient` instance which bypassed the configured proxy. It was consumed by injecting it using the ``@Named("systemHttpClient")`` annotation. Since the ``http_non_proxy_hosts`` configuration directive exists, which allows configuring hosts which bypass the proxy, it is not required anymore and not used internally either. Therefore it is removed. We advise any plugin author aware of the usage of this functionality in the plugin to remove the ``@Named`` annotation so the generic client is used instead.

Known Bugs and Limitations
==========================

* tbd
