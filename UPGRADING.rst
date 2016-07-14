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


Internal Metrics to MongoDB
---------------------------

Previous versions of Graylog included a (long deprecated) metrics reporter for writing internal `metrics <http://metrics.dropwizard.io/3.1.0/>`__ into MongoDB in a fixed interval of 1 second.

This feature has been removed completely and can be optionally pulled in by using the `Graylog Metrics Reporter Plugins <https://github.com/Graylog2/graylog-plugin-metrics-reporter>`_.


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


For Plugin Authors
------------------

Between 2.0 and 2.1 we also made changes to the Plugin API. These include:

* Removing ``org.graylog2.plugin.streams.Stream#getAlertCondition``, as it was faulty and not easily replaceable with a working version without breaking our separation of models and persistence services.

If you are maintaining a plugin that was originally written for 1.x or 2.0, you need to make sure that your plugin is still compiling and working under 2.1 or adapt it if necessary.

Changed Elasticsearch Cluster Status Behavior
---------------------------------------------

In previous versions Graylog stopped indexing into the current write index if the `Elasticsearch cluster status <http://docs.graylog.org/en/2.1/pages/configuration/elasticsearch.html#cluster-status-explained>`_ turned RED. Since 2.1 Graylog only checks the status of the current write index when it tries to index messages.

If the current write index is GREEN or YELLOW, Graylog will continue to index messages even though the overall cluster status is RED. This avoids Graylog downtimes when doing Elasticsearch maintenance or when older indices have problems.
