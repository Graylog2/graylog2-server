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


For Plugin Authors
------------------

Between 2.0 and 2.1 we also made changes to the Plugin API. These include:

* Removing ``org.graylog2.plugin.streams.Stream#getAlertCondition``, as it was faulty and not easily replaceable with a working version without breaking our separation of models and persistence services.

If you are maintaining a plugin that was originally written for 1.x or 2.0, you need to make sure that your plugin is still compiling and working under 2.1 or adapt it if necessary.
