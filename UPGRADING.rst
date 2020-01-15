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

Known Bugs and Limitations
==========================

  * Content Packs containing old Dashbords can not be installed in Graylog 3.2.
  * Some functionality of the search has been removed, namely:
    * Exporting a result set to CSV from the UI.
    * Retrieving the full query that is sent to Elasticsearch.
    * Retrieving the list of terms a message field value was indexed with.
    * The list of indices the current search used to generate results.
  * The "Show surrounding messages" action is not part of 3.2.0, but will be reimplemented in a next version.

