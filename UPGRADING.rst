*****************
Upgrading Graylog
*****************

.. _upgrade-from-20-to-21:

This file only contains the upgrade note for the upcoming release.
Please see `our documentation <http://docs.graylog.org/en/latest/pages/upgrade.html>`_
for the complete upgrade notes.

From 2.0 to 2.1
===============

HTTPS Setup
-----------

Previous versions of Graylog were automatically generating a private key/certificate pair for HTTPS if either the private key or the certificate (or both) for ``rest_tls_key_file``, ``rest_tls_cert_file``, ``web_tls_key_file``, or ``web_tls_cert_file`` couldn't be read. While this feature is very comfortable for inexperienced users, it has lots of serious drawbacks like very weak key sizes (only 1024 bits), being untrusted by all TLS libraries used by web browsers and other client software (because they are self-signed and not included in the system's CA/trust store), and problems with inter-node communications with other Graylog nodes.

Due to those shortcomings, the feature has been removed completely. Users need to use proper certificates or generate their own self-signed certificates and configure them with the appropriate settings, see `Using HTTPS <http://docs.graylog.org/en/2.0/pages/configuration/https.html>`_ for reference.


Web Interface Listener
----------------------

Graylog 2.0.x has been using separate listeners for the REST API and the web interface by default. The Graylog REST API on ``http://127.0.0.1:12900``, the Graylog web interface on ``http://127.0.0.1:9000``.
Beginning with Graylog 2.1.0 it is possible to run both the REST API and the web interface on the same host/port-combination and this is now the default. This means that the REST API is now running on ``http://127.0.0.1:9000/api/`` by default and the web interface is now running on ``http://127.0.0.1:9000/``.
Furthermore, all requests going to ``http://127.0.0.1:9000/api/`` requesting a content-type of ``text/html`` or ``application/xhtml+xml`` are redirected to the web interface, therefore making it even easier to set up Graylog and use it behind proxies, expose it externally etc.

Please take note that you can still run the REST API and the web interface on two separate listeners. If you are running a Graylog 2.0.x configuration specifying ``web_listen_uri`` explicitly and you want to keep that, you do not have to change anything.

Please also take note, that when you have configured ``rest_listen_uri`` and ``web_listen_uri`` to run on the same host/port-combination, the following configuration directives will have no effect:

  - ``web_enable_tls``, ``web_tls_cert_file``, ``web_tls_key_file``, ``web_tls_key_password`` (These will depend on the TLS configuration of the REST listener).
  - ``web_enable_cors``, ``web_enable_gzip``, ``web_thread_pool_size``, ``web_max_initial_line_length``, ``web_max_header_size`` (Those will depend on the corresponding settings of the REST listener).


Internal Metrics to MongoDB
---------------------------

Previous versions of Graylog included a (long deprecated) metrics reporter for writing internal `metrics <http://metrics.dropwizard.io/3.1.0/>`__ into MongoDB in a fixed interval of 1 second.

This feature has been removed completely and can be optionally pulled in by using the `Graylog Metrics Reporter Plugins <https://github.com/Graylog2/graylog-plugin-metrics-reporter>`_.


Configuration file changes
--------------------------

Network settings
^^^^^^^^^^^^^^^^

The network settings in the Graylog configuration file (``rest_listen_uri``, ``rest_transport_uri``, and ``web_listen_uri``) are now using the default ports for the HTTP (80) and HTTPS (443) if no custom port was given. Previously those settings were using the custom ports 12900 (Graylog REST API) and 9000 (Graylog web interface) if no explicit port was given.

Examples:

+-----------------------------------------------+------------------------------+-----------------------------+
| Configuration setting                         | Old effective URI            | New effective URI           |
+===============================================+==============================+=============================+
| ``rest_listen_uri = http://127.0.0.1:12900/`` | ``http://127.0.0.1:12900/``  | ``http://127.0.0.1:12900/`` |
| ``rest_listen_uri = http://127.0.0.1/``       | ``http://127.0.0.1:12900/``  | ``http://127.0.0.1:80/``    |
| ``rest_listen_uri = https://127.0.0.1/``      | ``https://127.0.0.1:12900/`` | ``https://127.0.0.1:443/``  |
+-----------------------------------------------+------------------------------+-----------------------------+


Graylog REST API
----------------

Removed resources
^^^^^^^^^^^^^^^^^

+-----------------------------+--------------------------------------------------------+ 
| Original resource           | Replacement                                            |
+=============================+========================================================+ 
| ``/system/buffers``         | ``/system/metrics/org.graylog2.buffers.input.size``    |
|                             | ``/system/metrics/org.graylog2.buffers.input.usage``   |
|                             | ``/system/metrics/org.graylog2.buffers.process.size``  |
|                             | ``/system/metrics/org.graylog2.buffers.process.usage`` |
|                             | ``/system/metrics/org.graylog2.buffers.output.size``   |
|                             | ``/system/metrics/org.graylog2.buffers.output.usage``  |
+-----------------------------+--------------------------------------------------------+ 
| ``/system/buffers/classes`` | None                                                   |
+-----------------------------+--------------------------------------------------------+ 


Removed index rotation/retention settings from "/system/configuration"
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The index rotation and retention settings have been moved to MongoDB in Graylog 2.0.0 but the representation of the old configuration options was still present in the ``/system/configuration`` resource.

In order to stay in sync with the actual configuration file, the following values have been removed:

* ``rotation_strategy``
* ``retention_strategy``
* ``elasticsearch_max_docs_per_index``
* ``elasticsearch_max_size_per_index``
* ``elasticsearch_max_time_per_index``
* ``elasticsearch_max_number_of_indices``


The retention and rotation configuration settings can be retrieved using the following resources:

* ``/system/indices/rotation/config``
* ``/system/indices/retention/config``

The retention strategy defaults to "Do nothing" unless an other strategy was explicitly configured. Please check the setting after upgrading.

For Plugin Authors
------------------

Between 2.0 and 2.1 we also made changes to the Plugin API. These include:

* Removing ``org.graylog2.plugin.streams.Stream#getAlertCondition``, as it was faulty and not easily replaceable with a working version without breaking our separation of models and persistence services.

If you are maintaining a plugin that was originally written for 1.x or 2.0, you need to make sure that your plugin is still compiling and working under 2.1 or adapt it if necessary.

UI Plugins
^^^^^^^^^^

The new app prefix feature requires some changes in UI plugins to make them work with that.

* ``import webpackEntry from 'webpack-entry';`` needs to be added at the very top of the ``src/web/index.jsx`` file
* The ``Routes.pluginRoute()`` function needs to be used instead of a literal string to build URLs for links and buttons

Please check the `updated documentation <INSERT-DOC-LINK-HERE>`_ for details.

Changed Elasticsearch Cluster Status Behavior
---------------------------------------------

In previous versions Graylog stopped indexing into the current write index if the `Elasticsearch cluster status <http://docs.graylog.org/en/2.1/pages/configuration/elasticsearch.html#cluster-status-explained>`_ turned RED. Since 2.1 Graylog only checks the status of the current write index when it tries to index messages.

If the current write index is GREEN or YELLOW, Graylog will continue to index messages even though the overall cluster status is RED. This avoids Graylog downtimes when doing Elasticsearch maintenance or when older indices have problems.

Changes in message field values trimming
----------------------------------------

Previous versions of Graylog were trimming message field values inconsistently, depending on the codec used. We have changed that behaviour in 2.1, so all message field values are trimmed by default. This means that leading or trailing whitespace of every field is removed during ingestion.

**Important**: This change will break your existing stream rules, extractors, and Drool rules if you are expecting leading or trailing white spaces in them. Please adapt them so they do not require those white spaces.
