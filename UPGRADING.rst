**************************
Upgrading to Graylog 3.2.x
**************************

.. _upgrade-from-30-to-32:

Migrating Dashboards
====================

Graylog 3.2 contains a massive overhaul of its dashboarding functionality, which comes with a different data structure for them. Therefore, when running Graylog 3.2 for the first time, existing dashboards are being migrated. We try to keep them the same way as before, with a few exceptions:

  * Quickvalues widgets configured to show both a pie chart and a data table are split up into two different widgets
  * Stacked Charts containing multiple series with different queries are split up by query. If a stacked chart contains 5 series, 3 with query "foo", 2 with query "bar, it is split up into two widgets, one containing all 3 "foo"-series, the other containing the 2 "bar"-series.
  * Widgets created using 3rd party plugins are migrated with their config, but unless the plugin author creates a corresponding plugin for 3.2, a placeholder is shown.


Known Bugs and Limitations
==========================

  * Content Packs containing old Dashbords can not be installed in Graylog 3.2.

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

New "bin_dir" and "data_dir" configuration parameters
=====================================================

We introduced two new configuration parameters related to file system paths.

- ``bin_dir`` config option points to the directory that contains scripts like ``graylogctl``.
- ``data_dir`` option configures the base directory for Graylog server state.

Please check the updated default ``graylog.conf`` configuration file for required changes to your existing file.


Removed support for Drools-based filters
========================================

For a long time, Graylog allowed to use `Drools <https://www.drools.org/>`_ to filter messages. Unfortunately, using Drools to perform complex filter logic came with a performance penalty and wasn't as flexible as we would have liked it to be.

Starting with Graylog 3.0.0, the support for Drools-based message filters has been removed from Graylog. The ``rules_file`` configuration setting has been removed accordingly.

We recommend migrating the Drools-based logic to `Processing Pipelines <http://docs.graylog.org/en/3.0/pages/pipelines.html>`_.


Drools-based blacklist
----------------------

Graylog provided undocumented blacklist-functionality based on Drools. This blacklist could only be modified via the Graylog REST API on the ``/filters/blacklist`` resource.

If you've been using this functionality, you'll have to migrate these blacklist rules to the `Processing Pipelines <http://docs.graylog.org/en/3.0/pages/pipelines.html>`_.

To check if you're using the Drools-based blacklist in Graylog prior to version 3.0.0, you can run the following command::

    # curl -u admin:password -H 'Accept: application/json' 'http://graylog.example.com/api/filters/blacklist?pretty=true'


String-based blacklist rule
^^^^^^^^^^^^^^^^^^^^^^^^^^^

Old blacklist rule::

    {
       "id" : "54e300001234123412340001",
       "type" : "string",
       "name" : "String Blacklist",
       "description" : "Drop messages based on case-insensitive string comparison",
       "fieldName" : "custom_field",
       "pattern" : "EXAMPLE pattern",
       "creator_user_id" : "admin",
       "created_at" : "2018-04-04T12:00:00.000Z"
    }

New pipeline rule::

    rule "string-blacklist"
    when
      has_field("custom_field") &&
      lowercase(to_string($message.custom_field)) == "example pattern"
    then
      drop_message();
    end

See also:

* `has_field() <http://docs.graylog.org/en/3.0/pages/pipelines/functions.html#has-field>`_
* `lowercase() <http://docs.graylog.org/en/3.0/pages/pipelines/functions.html#lowercase>`_
* `drop_message() <http://docs.graylog.org/en/3.0/pages/pipelines/functions.html#drop-message>`_

Regex-based blacklist rule
^^^^^^^^^^^^^^^^^^^^^^^^^^

Old blacklist rule::

    {
       "id" : "54e300001234123412340002",
       "type" : "regex",
       "name" : "Regex Blacklist",
       "description" : "Drop messages based on regular expression",
       "fieldName" : "custom_field",
       "pattern" : "^EXAMPLE.*",
       "creator_user_id" : "admin",
       "created_at" : "2018-04-04T12:00:00.000Z"
    }

New pipeline rule::

    rule "regex-blacklist"
    when
      has_field("custom_field") &&
      regex("^EXAMPLE.*", to_string($message.custom_field)).matches == true
    then
      drop_message();
    end

See also:

* `has_field() <http://docs.graylog.org/en/3.0/pages/pipelines/functions.html#has-field>`_
* `regex() <http://docs.graylog.org/en/3.0/pages/pipelines/functions.html#regex>`_
* `drop_message() <http://docs.graylog.org/en/3.0/pages/pipelines/functions.html#drop-message>`_

IP Range-based blacklist rule
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Old blacklist rule::

    {
       "id" : "54e300001234123412340003",
       "type" : "iprange",
       "name" : "IP Blacklist",
       "description" : "Drop messages based on IP address",
       "fieldName" : "custom_field",
       "pattern" : "192.168.0.0/16",
       "creator_user_id" : "admin",
       "created_at" : "2018-04-04T12:00:00.000Z"
    }

New pipeline rule::

    rule "ip-blacklist"
    when
      has_field("custom_field") &&
      cidr_match("192.168.0.0/16", to_ip($message.custom_field))
    then
      drop_message();
    end

See also:

* `has_field() <http://docs.graylog.org/en/3.0/pages/pipelines/functions.html#has-field>`_
* `to_ip() <http://docs.graylog.org/en/3.0/pages/pipelines/functions.html#to-ip>`_
* `cidr_match() <http://docs.graylog.org/en/3.0/pages/pipelines/functions.html#cidr-match>`_
* `drop_message() <http://docs.graylog.org/en/3.0/pages/pipelines/functions.html#drop-message>`_


Changed metrics name for stream rules
=====================================

The name of the metrics for stream rules have been changed to include the stream ID which helps identifying the actual stream they are related to.

Old metric name::

    org.graylog2.plugin.streams.StreamRule.${stream-rule-id}.executionTime

New metric name::

    org.graylog2.plugin.streams.Stream.${stream-id}.StreamRule.${stream-rule-id}.executionTime


Email alarm callback default settings
=====================================

The defaults of the configuration settings for the email alarm callback with regard to encrypted connections have been changed.

+-----------------------------+-------------+-------------+
| Setting                     | Old default | New default |
+=============================+=============+=============+
| ``transport_email_use_tls`` | ``false``   | ``true``    |
+-----------------------------+-------------+-------------+
| ``transport_email_use_ssl`` | ``true``    | ``false``   |
+-----------------------------+-------------+-------------+

Furthermore, it's not possible anymore to enable both settings (SMTP with STARTTLS and SMTP over SSL) at the same time because this led to errors at runtime when Graylog tried to upgrade the connection to TLS with STARTTLS in an already existing SMTPS connection.

Most SMTP services prefer SMTP with STARTTLS to provide an encrypted connection.


Setting initial configuration on widget's configurationCreateComponent
======================================================================

Widget plugins that want to customize the create modal by adding some custom inputs need to additionally set the
initial configuration for them. Before, we accessed the component's ``getInitialConfiguration()`` when opening the
creation modal form, but this is now not possible due to performance improvements.

In 3.0, setting the initial widget configuration on the create component can be achieved in two different ways:

Setting ``initialConfiguration`` class property
---------------------------------------------
This is the preferred method, and it should be used every time configuration does not depend on any external state
or props. Example::

    static initialConfiguration = { shouldShowChart: true, description: 'Initial description' };


Calling the ``setInitialConfiguration`` prop
--------------------------------------------
``WidgetCreationModal`` passes a function called ``setInitialConfiguration`` to the ``configurationCreateComponent``
defined for the widget. That function can be called on the ``constructor`` or ``componentDidMount`` of the custom
component to set the initial configuration values if any of them is derived from state or other props.
Note that any configuration key set through ``setInitialConfiguration`` will have precedence over configuration keys
set by ``initialConfiguration`` and will override existing configuration keys.
Example::

   static initialConfiguration = { key: value, test: false };

   constructor(props) {
     super(props);
     props.setInitialConfiguration({ field: props.fields[0], test: true });
   }

   /* The effective initial configuration would be: { key: value, field: props.fields[0], test: true } */

