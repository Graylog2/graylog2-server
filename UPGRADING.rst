**************************
Upgrading to Graylog 2.3.x
**************************

.. _upgrade-from-22-to-23:

This file only contains the upgrade note for the upcoming release.
Please see `our documentation <http://docs.graylog.org/en/latest/pages/upgrade.html>`_
for the complete upgrade notes.

Graylog switches to Elasticsearch HTTP client
=============================================

In all prior versions, Graylog used the Elasticsearch node client to connect to an Elasticsearch cluster, which was acting as a client-only Elasticsearch node. For compatibility reasons of the used binary transfer protocol, the range of Elasticsearch versions Graylog could connect to was limited. For more information and differences between the different ways to connect to Elasticsearch, you can check the `Elasticsearch documentation <https://www.elastic.co/guide/en/elasticsearch/guide/current/_talking_to_elasticsearch.html>`_.

Starting with version 2.3.0, we are switching over to using a lightweight HTTP client, which is almost version-agnostic. The biggest change is that it does not connect to the Elasticsearch native protocol port (defaulting to 9300/tcp), but the Elasticsearch HTTP port (defaulting to 9200/tcp).

Due to the differences in connecting to the Elasticsearch cluster, configuring Graylog has changed. These configuration settings have been removed::

  elasticsearch_cluster_discovery_timeout
  elasticsearch_cluster_name
  elasticsearch_config_file
  elasticsearch_discovery_initial_state_timeout
  elasticsearch_discovery_zen_ping_unicast_hosts
  elasticsearch_http_enabled
  elasticsearch_network_bind_host
  elasticsearch_network_host
  elasticsearch_network_publish_host
  elasticsearch_node_data
  elasticsearch_node_master
  elasticsearch_node_name_prefix
  elasticsearch_path_data
  elasticsearch_path_home
  elasticsearch_transport_tcp_port

The following configuration options are now being used to configure connectivity to Elasticsearch:

+----------------------------------------------------+-----------+--------------------------------------------------------------+-----------------------------+
| Config Setting                                     | Type      | Comments                                                     | Default                     |
+====================================================+===========+==============================================================+=============================+
| ``elasticsearch_connect_timeout``                  | Duration  | Timeout when connection to individual Elasticsearch hosts    | ``10s`` (10 Seconds)        |
+----------------------------------------------------+-----------+--------------------------------------------------------------+-----------------------------+
| ``elasticsearch_hosts``                            | List<URI> | Comma-separated list of URIs of Elasticsearch hosts          | ``http://127.0.0.1:9200``   |
+----------------------------------------------------+-----------+--------------------------------------------------------------+-----------------------------+
| ``elasticsearch_idle_timeout``                     | Duration  | Timeout after which idle connections are terminated          | ``-1s`` (Never)             |
+----------------------------------------------------+-----------+--------------------------------------------------------------+-----------------------------+
| ``elasticsearch_max_total_connections``            | int       | Maximum number of total Elasticsearch connections            | ``20``                      |
+----------------------------------------------------+-----------+--------------------------------------------------------------+-----------------------------+
| ``elasticsearch_max_total_connections_per_route``  | int       | Maximum number of Elasticsearch connections per route/host   | ``2``                       |
+----------------------------------------------------+-----------+--------------------------------------------------------------+-----------------------------+
| ``elasticsearch_socket_timeout``                   | Duration  | Timeout when sending/receiving from Elasticsearch connection | ``60s`` (60 Seconds)        |
+----------------------------------------------------+-----------+--------------------------------------------------------------+-----------------------------+
| ``elasticsearch_discovery_enabled``                | boolean   | Enable automatic Elasticsearch node discovery                | ``false``                   |
+----------------------------------------------------+-----------+--------------------------------------------------------------+-----------------------------+
| ``elasticsearch_discovery_filter``                 | String    | Filter by node attributes for the discovered nodes           | empty (use all nodes)       |
+----------------------------------------------------+-----------+--------------------------------------------------------------+-----------------------------+
| ``elasticsearch_discovery_frequency``              | Duration  | Frequency of the Elasticsearch node discovery                | ``30s`` (30 Seconds)        |
+----------------------------------------------------+-----------+--------------------------------------------------------------+-----------------------------+

In most cases, the only configuration setting that needs to be set explicitly is ``elasticsearch_hosts``. All other configuration settings should be tweaked only in case of errors.

.. warn:: The automatic node discovery does not work if Elasticsearch requires authentication, e. g. when using Shield (X-Pack).

.. caution:: Graylog does not react to externally triggered index changes (creating/closing/reopening/deleting an index) anymore. All of these actions need to be performed through the Graylog REST API in order to retain index consistency.


Special note for upgrading from an existing Graylog setup with a new Elasticsearch cluster
------------------------------------------------------------------------------------------

If you are upgrading the Elasticsearch cluster of an existing Graylog setup without migrating the indices, your Graylog setup contains stale index ranges causing nonexisting index errors upon search/alerting. To remediate this, you need to manually trigger an index range recalculation for all index sets once. This is possible using the web interface using the System->Indices functionality or by using the REST API using the ``/system/indices/ranges/<index set id>/rebuild`` endpoint.

Graylog REST API
================

Rotation and Retention strategies
---------------------------------

The deprecated HTTP resources at ``/system/indices/rotation/config`` and ``/system/indices/retention/config``, which didn't work since Graylog 2.2.0, have been removed.

These settings are part of the index set configuration and can be configured under ``/system/indices/index_sets``.

Stream List Response structure does not include `in_grace` field anymore
------------------------------------------------------------------------

The response to ``GET /streams``, ``GET /streams/<id>`` & ``PUT /streams/<id>`` does not contain the ``in_grace`` field for configured alert conditions anymore.

The value of this flag can be retrieved using the ``GET /alerts/conditions`` endpoint, or per stream using the ``GET /streams/<streamId>/alerts/conditions`` endpoint.
