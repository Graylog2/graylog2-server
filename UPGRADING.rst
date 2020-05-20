**************************
Upgrading to Graylog 3.3.x
**************************

.. _upgrade-from-32-to-33:

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
