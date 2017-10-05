**************************
Upgrading to Graylog 3.0.x
**************************

.. _upgrade-from-24-to-30:

This file only contains the upgrade note for the upcoming release.
Please see `our documentation <http://docs.graylog.org/en/latest/pages/upgrade.html>`_
for the complete upgrade notes.

Simplified HTTP interface configuration
=======================================

Graylog used to have a lot of different settings regarding the various HTTP interfaces it provides, namely the Graylog REST API and the Graylog web interface.

This mostly originates from the fact that Graylog used to consist of two components before Graylog 2.0.0, a server component and a separate web interface.

The changes in this release finally merge the HTTP listeners for the Graylog REST API and web interface into a single HTTP listener, which should make the initial configuration of Graylog simpler and reduce errors caused by conflicting settings.

The path of the Graylog REST API is now hard-coded to ``/api``, so if you're still using the legacy URI on port 12900/tcp or have been using a custom path (via the ``rest_listen_uri`` or ``rest_transport_uri`` settings), you'll have to update the URI used to access the Graylog REST API.

For a more detailed description of the new HTTP settings, please consult the annotated `Graylog configuration file <https://github.com/Graylog2/graylog2-server/blob/d9bb656275eeac7027e3fe12d9ee1b6a0905dcd1/misc/graylog.conf#L79-L81>`__.


Overview of deprecated Graylog REST API settings:

+----------------------------------+----------------------------------+--------------------------------+
| Deprecated Setting               | New Setting                      | Default                        |
+==================================+==================================+================================+
| ``rest_listen_uri``              | ``http_bind_address``            | ``127.0.0.1:9000``             |
+----------------------------------+----------------------------------+--------------------------------+
| ``rest_transport_uri``           | ``http_publish_uri``             | ``http://$http_bind_address/`` |
+----------------------------------+----------------------------------+--------------------------------+
| ``web_endpoint_uri``             | ``http_external_uri``            | ``$http_publish_uri``          |
+----------------------------------+----------------------------------+--------------------------------+
| ``rest_enable_cors``             | ``http_enable_cors``             | ``true``                       |
+----------------------------------+----------------------------------+--------------------------------+
| ``rest_enable_gzip``             | ``http_enable_gzip``             | ``true``                       |
+----------------------------------+----------------------------------+--------------------------------+
| ``rest_max_header_size``         | ``http_max_header_size``         | ``8192``                       |
+----------------------------------+----------------------------------+--------------------------------+
| ``rest_max_initial_line_length`` | ``http_max_initial_line_length`` | ``4096``                       |
+----------------------------------+----------------------------------+--------------------------------+
| ``rest_thread_pool_size``        | ``http_thread_pool_size``        | ``16``                         |
+----------------------------------+----------------------------------+--------------------------------+
| ``rest_enable_tls``              | ``http_enable_tls``              | ``false``                      |
+----------------------------------+----------------------------------+--------------------------------+
| ``rest_tls_cert_file``           | ``http_tls_cert_file``           | Empty                          |
+----------------------------------+----------------------------------+--------------------------------+
| ``rest_tls_key_file``            | ``http_tls_key_file``            | Empty                          |
+----------------------------------+----------------------------------+--------------------------------+
| ``rest_tls_key_password``        | ``http_tls_key_password``        | Empty                          |
+----------------------------------+----------------------------------+--------------------------------+


Overview of deprecated Graylog web interface settings:

+---------------------------------+----------------------------------+--------------------+
| Deprecated Setting              | New Setting                      | Default            |
+=================================+==================================+====================+
| ``web_enable``                  | None                             |                    |
+---------------------------------+----------------------------------+--------------------+
| ``web_listen_uri``              | ``http_bind_address``            | ``127.0.0.1:9000`` |
+---------------------------------+----------------------------------+--------------------+
| ``web_enable_cors``             | ``http_enable_cors``             | ``true``           |
+---------------------------------+----------------------------------+--------------------+
| ``web_enable_gzip``             | ``http_enable_gzip``             | ``true``           |
+---------------------------------+----------------------------------+--------------------+
| ``web_max_header_size``         | ``http_max_header_size``         | ``8192``           |
+---------------------------------+----------------------------------+--------------------+
| ``web_max_initial_line_length`` | ``http_max_initial_line_length`` | ``4096``           |
+---------------------------------+----------------------------------+--------------------+
| ``web_thread_pool_size``        | ``http_thread_pool_size``        | ``16``             |
+---------------------------------+----------------------------------+--------------------+
| ``web_enable_tls``              | ``http_enable_tls``              | ``false``          |
+---------------------------------+----------------------------------+--------------------+
| ``web_tls_cert_file``           | ``http_tls_cert_file``           | Empty              |
+---------------------------------+----------------------------------+--------------------+
| ``web_tls_key_file``            | ``http_tls_key_file``            | Empty              |
+---------------------------------+----------------------------------+--------------------+
| ``web_tls_key_password``        | ``http_tls_key_password``        | Empty              |
+---------------------------------+----------------------------------+--------------------+
