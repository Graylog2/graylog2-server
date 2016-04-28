*****************
Upgrading Graylog
*****************

.. _upgrade-from-1-to-2:

From 1.x to 2.x
===============

Elasticsearch 2.x
-----------------

The embedded Elasticsearch node being used by Graylog has been upgraded to Elasticsearch 2.x which includes some breaking changes.
Graylog 2.x does not work with Elasticsearch 1.x anymore and cannot communicate with existing Elasticsearch 1.x clusters.

Please see `Breaking changes in Elasticsearch 2.x <https://www.elastic.co/guide/en/elasticsearch/reference/2.0/breaking-changes.html>`_ for details.

The blog article `Key points to be aware of when upgrading from Elasticsearch 1.x to 2.x <https://www.elastic.co/blog/key-point-to-be-aware-of-when-upgrading-from-elasticsearch-1-to-2>`_ also contains interesting information about the upgrade path from Elasticsearch 1.x to 2.x.

Multicast Discovery
^^^^^^^^^^^^^^^^^^^

Multicast discovery has been removed from Elasticsearch 2.x (although it is still provided as an Elasticsearch plugin for now).

To reflect this change, the ``elasticsearch_discovery_zen_ping_unicast_hosts`` now has to contain the address of at least one
Elasticsearch node in the cluster which Graylog can connect to.

Index range types
^^^^^^^^^^^^^^^^^

.. note:: This step needs to be performed before the update to Elasticsearch 2.x!

Some Graylog versions stored meta information about indices in elasticsearch, alongside the messages themselves. Since Elasticsearch 2.0
having multiple types with conflicting mappings is no longer possible, which means that the ``index_range`` type must be removed before
upgrading to Elasticsearch 2.x.

Find out if your setup is affected by running (replace ``$elasticsearch`` with the address of one of your Elasticsearch nodes)
``curl -XGET $elasticsearch:9200/_all/_mapping/index_range; echo``

If the output is ``{}`` you are not affected and can skip this step.

Otherwise, you need to delete the ``index_range`` type, Graylog does not use it anymore.

As Graylog sets older indices to read-only, first we need to remove the write block on those indices.
Since we'll be working with Elasticsearch's JSON output, we recommend installing the ``jq`` utility which should be
available on all popular package managers or directly at `GitHub <https://stedolan.github.io/jq/>`_.

::

    for i in `curl -s -XGET $elasticsearch:9200/_all/_mapping/index_range | jq -r "keys[]"`; do
        echo -n "Updating index $i: "
        echo -n "curl -XPUT $elasticsearch:9200/$i/_settings -d '{\"index.blocks.read_only\":false, \"index.blocks.write\":false}' : "
        curl -XPUT $elasticsearch:9200/$i/_settings -d '{"index.blocks.read_only":false, "index.blocks.write":false}'
        echo
    done

The output for each of the curl commands should be ``{"acknowledged":true}``.
Next we have to delete the ``index_range`` mapping. We can perform this via the next command.

.. note:: We strongly recommend to perform this on a single index before running this bulk command.
          This operation can be expensive to perform if you have a lot of affected indices.

::

    for i in `curl -s -XGET $elasticsearch:9200/_all/_mapping/index_range | jq -r "keys[]"`; do
        echo -n "Updating index $i: "
        curl -XDELETE $elasticsearch:9200/$i/index_range
        echo
    done

It is not strictly necessary to set the indices back to read only, but if you prefer to do that, note the index names and
commands during the first step and change the ``false`` into ``true``.


Graylog Index Template
^^^^^^^^^^^^^^^^^^^^^^

Graylog applies a custom `index template <https://www.elastic.co/guide/en/elasticsearch/reference/2.0/indices-templates.html>`_ to ensure
that the indexed messages adhere to a specific schema.

Unfortunately the index template being used by Graylog 1.x is incompatible with Elasticsearch 2.x and has to be removed prior to upgrading.

In order to `delete the index template <https://www.elastic.co/guide/en/elasticsearch/reference/2.0/indices-templates.html#delete>`_ the
following curl command has to be issued against on of the Elasticsearch nodes::

    curl -X DELETE http://localhost:9200/_template/graylog-internal

Graylog will automatically create the new index template on the next startup.

Dots in field names
^^^^^^^^^^^^^^^^^^^

One of the most important breaking changes in Elasticsearch 2.x is that
`field names may not contain dots <https://www.elastic.co/guide/en/elasticsearch/reference/2.0/breaking_20_mapping_changes.html#_field_names_may_not_contain_dots>`_ anymore.

Using the `Elasticsearch Migration Plugin <https://github.com/elastic/elasticsearch-migration>`_ might help to highlight
some potential pitfalls if an existing Elasticsearch 1.x cluster should be upgraded to Elasticsearch 2.x.


MongoDB
-------

Graylog 2.x requires MongoDB 2.4 or newer. We recommend using MongoDB 3.x and the
`WiredTiger storage engine <https://docs.mongodb.org/v3.2/core/wiredtiger/>`_.

When upgrading from MongoDB 2.0 or 2.2 to a supported version, make sure to read the
`Release Notes <https://docs.mongodb.org/manual/release-notes/>`_ for the particular version.


Log4j 2 migration
-----------------

Graylog switched its logging backend from `Log4j 1.2 <https://logging.apache.org/log4j/1.2/>`_
to `Log4j 2 <https://logging.apache.org/log4j/2.x/>`_.

Please refer to the `Log4j Migration Guide <https://logging.apache.org/log4j/2.x/manual/migration.html>`_ for information
on how to update your existing logging configuration.


Dead Letters feature removed
----------------------------

The Dead Letters feature, which stored messages that couldn't be indexed into Elasticsearch for various reasons, has been removed.

This feature has been disabled by default. If you have enabled the feature the configuration file, please check the ``dead_letters_enabled``
collection in MongoDB and remove it afterwards.


Removed configuration settings
------------------------------

Some settings, which have been deprecated in previous versions, have finally been removed from the Graylog configuration file.

.. list-table:: Removed configuration settings
    :header-rows: 1

    * - Setting name
      - Replacement
    * - ``mongodb_host``
      - ``mongodb_uri``
    * - ``mongodb_port``
      - ``mongodb_uri``
    * - ``mongodb_database``
      - ``mongodb_uri``
    * - ``mongodb_useauth``
      - ``mongodb_uri``
    * - ``mongodb_user``
      - ``mongodb_uri``
    * - ``mongodb_password``
      - ``mongodb_uri``
    * - ``elasticsearch_node_name``
      - ``elasticsearch_node_name_prefix``
    * - ``collector_expiration_threshold``
      - (moved to collector plugin)
    * - ``collector_inactive_threshold``
      - (moved to collector plugin)
    * - ``rotation_strategy``
      - UI in web interface (System/Indices)
    * - ``retention_strategy``
      - UI in web interface (System/Indices)
    * - ``elasticsearch_max_docs_per_index``
      - UI in web interface (System/Indices)
    * - ``elasticsearch_max_size_per_index``
      - UI in web interface (System/Indices)
    * - ``elasticsearch_max_time_per_index``
      - UI in web interface (System/Indices)
    * - ``elasticsearch_max_number_of_indices``
      - UI in web interface (System/Indices)
    * - ``dead_letters_enabled``
      - None


Changed configuration defaults
------------------------------

For better consistency, the defaults of some configuration settings have been changed after the project has
been renamed from *Graylog2* to *Graylog*.

.. list-table:: Configuration defaults
    :header-rows: 1

    * - Setting name
      - Old default
      - New default
    * - ``elasticsearch_cluster_name``
      - ``graylog2``
      - ``graylog``
    * - ``elasticsearch_node_name``
      - ``graylog2-server``
      - ``graylog-server``
    * - ``elasticsearch_index_prefix``
      - ``graylog2``
      - ``graylog``
    * - ``elasticsearch_discovery_zen_ping_unicast_hosts``
      - empty
      - ``127.0.0.1:9300``
    * - ``elasticsearch_discovery_zen_ping_multicast_enabled``
      - ``true``
      - ``false``
    * - ``mongodb_uri``
      - ``mongodb://127.0.0.1/graylog2``
      - ``mongodb://localhost/graylog``


Changed prefixes for configuration override
-------------------------------------------

In the past it was possible to override configuration settings in Graylog using environment
variables or Java system properties with a specific prefix.

For better consistency, these prefixes have been changed after the project has been renamed
from *Graylog2* to *Graylog*.

.. list-table:: Configuration override prefixes
    :header-rows: 1

    * - Override
      - Old prefix
      - New prefix
      - Example
    * - Environment variables
      - ``GRAYLOG2_``
      - ``GRAYLOG_``
      - ``GRAYLOG_IS_MASTER``
    * - System properties
      - ``graylog2.``
      - ``graylog.``
      - ``graylog.is_master``

REST API Changes
----------------

The output ID key for the list of outputs in the ``/streams/*`` endpoints has been changed from ``_id`` to ``id``.

.. code-block:: javascript
   :emphasize-lines: 6

    {
      "id": "564f47c41ec8fe7d920ef561",
      "creator_user_id": "admin",
      "outputs": [
        {
          "id": "56d6f2cce45e0e52d1e4b9cb", // ==> Changed from `_id` to `id`
          "title": "GELF Output",
          "type": "org.graylog2.outputs.GelfOutput",
          "creator_user_id": "admin",
          "created_at": "2016-03-02T14:03:56.686Z",
          "configuration": {
            "hostname": "127.0.0.1",
            "protocol": "TCP",
            "connect_timeout": 1000,
            "reconnect_delay": 500,
            "port": 12202,
            "tcp_no_delay": false,
            "tcp_keep_alive": false,
            "tls_trust_cert_chain": "",
            "tls_verification_enabled": false
          },
          "content_pack": null
        }
      ],
      "matching_type": "AND",
      "description": "All incoming messages",
      "created_at": "2015-11-20T16:18:12.416Z",
      "disabled": false,
      "rules": [],
      "alert_conditions": [],
      "title": "ALL",
      "content_pack": null
    }
