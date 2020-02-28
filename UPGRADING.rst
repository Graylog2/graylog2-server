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
