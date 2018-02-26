**************************
Upgrading to Graylog 3.0.x
**************************

.. _upgrade-from-24-to-30:

This file only contains the upgrade note for the upcoming release.
Please see `our documentation <http://docs.graylog.org/en/latest/pages/upgrade.html>`_
for the complete upgrade notes.

Elasticsearch Version Requirements
==================================

Graylog 3.0 drops support for Elasticsearch versions before 5.6.x. That means you have to upgrade Elasticsearch to at least version 5.6.5 before upgrading Graylog to version 3.0. Make sure to read the Elasticsearch upgrade guides before doing that.

Simplified HTTP interface configuration
=======================================

Graylog used to have a lot of different settings regarding the various HTTP interfaces it provides, namely the Graylog REST API and the Graylog web interface.

This mostly originates from the fact that Graylog used to consist of two components before Graylog 2.0.0, a server component and a separate web interface.

The changes in this release finally merge the HTTP listeners for the Graylog REST API and web interface into a single HTTP listener, which should make the initial configuration of Graylog simpler and reduce errors caused by conflicting settings.

The path of the Graylog REST API is now hard-coded to ``/api``, so if you're still using the legacy URI on port 12900/tcp or have been using a custom path (via the ``rest_listen_uri`` or ``rest_transport_uri`` settings), you'll have to update the URI used to access the Graylog REST API.

If you are using a reverse proxy in front of Graylog (like nginx) and configured it to set the ``X-Graylog-Server-URL`` HTTP header, you have to remove the ``api/`` suffix because that is now the default. (as mentioned above)

Example::

    # This nginx setting in Graylog <3.0 ...
    header_upstream X-Graylog-Server-URL http://{host}/api

    # ... needs to be changed to the following with Graylog 3.0
    header_upstream X-Graylog-Server-URL http://{host}/

For a more detailed description of the new HTTP settings, please consult the annotated `Graylog configuration file <https://github.com/Graylog2/graylog2-server/blob/d9bb656275eeac7027e3fe12d9ee1b6a0905dcd1/misc/graylog.conf#L79-L81>`__.


Overview of removed Graylog REST API settings:

+----------------------------------+----------------------------------+--------------------------------+
| Removed Setting                  | New Setting                      | Default                        |
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


Overview of removed Graylog web interface settings:

+---------------------------------+----------------------------------+--------------------+
| Removed Setting                 | New Setting                      | Default            |
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

Plugins merged into the Graylog server
======================================

Starting with Graylog 3.0.0, the following official plugins were merged into the Graylog server:

- `Beats Input <https://github.com/Graylog2/graylog-plugin-beats>`_
- `CEF Input <https://github.com/Graylog2/graylog-plugin-cef>`_
- `Collector Plugin <https://github.com/Graylog2/graylog-plugin-collector>`_
- `Enterprise Integration Page <https://github.com/Graylog2/graylog-plugin-enterprise-integration>`_
- `Map Widget <https://github.com/Graylog2/graylog-plugin-map-widget>`_
- `NetFlow Input <https://github.com/Graylog2/graylog-plugin-netflow>`_
- `Pipeline Processor <https://github.com/Graylog2/graylog-plugin-pipeline-processor>`_

That means these plugins are not available as separate plugins anymore. If you manually update your Graylog installation (without using operating system packages), make sure to remove all old plugin files from the `plugin_dir <http://docs.graylog.org/en/3.0/pages/configuration/server.conf.html>`_ folder.

The old issues in these repositories are still available for reference but new issues should only be created in the `Graylog server issue tracker <https://github.com/Graylog2/graylog2-server/issues>`_.

The following HTTP API paths changed due to the plugin merge:

+---------------------------------------------------------------------------------------------+-----------------------------------------------+
| Old Path                                                                                    | New Path                                      |
+=============================================================================================+===============================================+
| ``/plugins/org.graylog.plugins.map/mapdata``                                                | ``/search/mapdata``                           |
+---------------------------------------------------------------------------------------------+-----------------------------------------------+
| ``/plugins/org.graylog.plugins.pipelineprocessor/system/pipelines/pipeline``                | ``/system/pipelines/pipeline``                |
+---------------------------------------------------------------------------------------------+-----------------------------------------------+
| ``/plugins/org.graylog.plugins.pipelineprocessor/system/pipelines/pipeline/parse``          | ``/system/pipelines/pipeline/parse``          |
+---------------------------------------------------------------------------------------------+-----------------------------------------------+
| ``/plugins/org.graylog.plugins.pipelineprocessor/system/pipelines/rule``                    | ``/system/pipelines/rule``                    |
+---------------------------------------------------------------------------------------------+-----------------------------------------------+
| ``/plugins/org.graylog.plugins.pipelineprocessor/system/pipelines/rule/functions``          | ``/system/pipelines/rule/functions``          |
+---------------------------------------------------------------------------------------------+-----------------------------------------------+
| ``/plugins/org.graylog.plugins.pipelineprocessor/system/pipelines/rule/multiple``           | ``/system/pipelines/rule/multiple``           |
+---------------------------------------------------------------------------------------------+-----------------------------------------------+
| ``/plugins/org.graylog.plugins.pipelineprocessor/system/pipelines/rule/parse``              | ``/system/pipelines/rule/parse``              |
+---------------------------------------------------------------------------------------------+-----------------------------------------------+
| ``/plugins/org.graylog.plugins.pipelineprocessor/system/pipelines/connections``             | ``/system/pipelines/connections``             |
+---------------------------------------------------------------------------------------------+-----------------------------------------------+
| ``/plugins/org.graylog.plugins.pipelineprocessor/system/pipelines/connections/to_stream``   | ``/system/pipelines/connections/to_stream``   |
+---------------------------------------------------------------------------------------------+-----------------------------------------------+
| ``/plugins/org.graylog.plugins.pipelineprocessor/system/pipelines/connections/to_pipeline`` | ``/system/pipelines/connections/to_pipeline`` |
+---------------------------------------------------------------------------------------------+-----------------------------------------------+
| ``/plugins/org.graylog.plugins.pipelineprocessor/system/pipelines/simulate``                | ``/system/pipelines/simulate``                |
+---------------------------------------------------------------------------------------------+-----------------------------------------------+
