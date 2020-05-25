**************************
Upgrading to Graylog 4.0.x
**************************

.. _upgrade-from-33-to-40:

Disable Cross-Origin Requests by Default
========================================

For improved security, Cross-Origin requests towards the API server are now disallowed by default.
In the rare case, that your setup is serving the frontend assets from a different
origin than the server, you can reenable this by with ``http_enable_cors = true`` in ``graylog.conf``.


