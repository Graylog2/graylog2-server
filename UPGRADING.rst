**************************
Upgrading to Graylog 3.2.x
**************************

Notes for plugin authors:

Prior to 3.2.0, it was possible to consume a special `OkHttpClient` instance which bypassed the configured proxy. It was consumed by injecting it using the ``@Named("systemHttpClient")`` annotation. Since the ``http_non_proxy_hosts`` configuration directive exists, which allows to blacklist hosts so access to them is bypassing the proxy, it is not required anymore and not used internally either. Therefore it is removed. We advise any plugin author aware of the usage of this functionality in the plugin to remove the ``@Named`` annotation so the generic client is used instead.

**************************
Upgrading to Graylog 3.3.x
**************************

.. _upgrade-from-32-to-33:

API Access Token Encryption
===========================

For improved security, all API access tokens will now be stored encrypted in the database. Existing API tokens will automatically be encrypted by a database migration on Graylog server startup.

.. warning:: The token encryption is using the ``password_secret`` value from ``graylog.conf`` (or ``/etc/graylog/server/server.conf``) as encryption key. All Graylog nodes in the cluster need to have the same value configured for that option to make sure encryption/decryption works correctly. (if the values differ across your nodes, use the one from the master node for all other nodes)

Known Bugs and Limitations
==========================

* tbd
