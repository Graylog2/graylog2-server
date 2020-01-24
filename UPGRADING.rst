**************************
Upgrading to Graylog 3.2.x
**************************

.. _upgrade-from-30-to-32:

.. note:: Graylog 3.2.0 comes with a number of migrations that change parts of your existing data fundamentally. Please make sure to have a recent backup of your MongoDB that you can rollback to, before attempting to upgrade from an earlier version.

Migrating Dashboards
====================

Graylog 3.2 contains a massive overhaul of its dashboarding functionality, which comes with a different data structure for them. Therefore, when running Graylog 3.2 for the first time, existing dashboards are being migrated. We try to keep them the same way as before, with a few exceptions:

  * Quickvalues widgets configured to show both a pie chart and a data table are split up into two different widgets
  * Stacked Charts containing multiple series with different queries are split up by query. If a stacked chart contains 5 series, 3 with query "foo", 2 with query "bar, it is split up into two widgets, one containing all 3 "foo"-series, the other containing the 2 "bar"-series.
  * Widgets created using 3rd party plugins are migrated with their config, but unless the plugin author creates a corresponding plugin for 3.2, a placeholder is shown.

Changed Default TLS Protocols
=============================

Graylog 3.2 will ship with a secure default of supported TLS protocols.
This means that TLS 1.0 and TLS 1.1 won't be supported anymore. The new default setting affects all TLS enabled services such as message inputs or the graylog web interface.
If needed, you can re-enable old TLS protocols with the newly introduced ``enabled_tls_protocols`` setting.

Indexing Requests use HTTP Expect: 100-Continue Header
======================================================

Messages indexing requests to Elasticsearch are now executed with a HTTP Expect-Continue header.
For the unlikely case that this is creating problems, it can be disabled using the newly introduced ``elasticsearch_use_expect_continue`` setting.

Accounted Message Size Field
============================

Every message now includes the ``gl2_accounted_message_size`` field. To make sure this field will be created with the correct data type in all active write indices, the mapping of these indices needs to be updated. New indices created by index rotation will automatically have the correct mapping because the index template got updated automatically.

.. warning:: The following steps need to be executed **before** starting the server with the 3.2.0 version!

All of the following commands need to be executed against one of the Elasticsearch servers in the cluster.

First, a list of all active write indices is needed::

  curl -s localhost:9200/_cat/aliases/*_deflector?h=index

For each of the index names returned by the previous command, the following command needs to be executed (``<active-write-index-name>`` in the URL needs to be replaced with the actual index name)::

  curl -s -X PUT --data '{"properties":{"gl2_accounted_message_size":{"type": "long"}}}' -H Content-Type:application/json localhost:9200/<active-write-index-name>/_mapping/message

The two steps could also be combined::

  for index in `curl -s localhost:9200/_cat/aliases/*_deflector?h=index`; do curl -s -X PUT --data '{"properties":{"gl2_accounted_message_size":{"type": "long"}}}' -H Content-Type:application/json localhost:9200/$index/_mapping/message ; done'

The Graylog servers can now be restarted with the 3.2.0 version.

Known Bugs and Limitations
==========================

  * Content Packs containing old Dashbords can not be installed in Graylog 3.2.
  * Some functionality of the search has been removed, namely:
    * Exporting a result set to CSV from the UI.
    * Retrieving the full query that is sent to Elasticsearch.
    * Retrieving the list of terms a message field value was indexed with.
    * The list of indices the current search used to generate results.
    * The count of all received messages displayed next to the search.
      We will add the count again, once the calculation works as expected.
      As a workaround a message count widget can be added to the search.
  * The "Show surrounding messages" action is not part of 3.2.0, but will be reimplemented in a next version.

