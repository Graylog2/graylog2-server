**************************
Upgrading to Graylog 4.1.x
**************************

.. _upgrade-from-40-to-41:

.. contents:: Overview
   :depth: 3
   :backlinks: top

.. warning:: Please make sure to create a MongoDB database backup before starting the upgrade to Graylog 4.1!

Breaking Changes
================

The limit parameter in the legacy search api (``/search/universal/(absolute|keyword|relative)``) semantics has changed
to fix an inconsistency introduced in ``4.0``: prior to ``4.0``, ``0`` meant "no limit", with ``4.0`` this changed to ``-1``
and ``0`` for "empty result". With 4.1 this has been fixed to work again like in the past but the underlying
``Searches#scroll`` method has been tagged as ``@deprecated`` now, too.

Changes to the Elasticsearch Support
------------------------------------

When you have version-probing for the used Elasticsearch version enabled, and Graylog starts up but can not
connect to ES, the startup stopped immediately with v4.0 and prior. Starting from 4.1 the default behaviour is,
that Graylog retries connecting with a delay until it can connect to Elasticsearch. See the Elasticsearch
configuration_ for details.

.. _configuration: https://docs.graylog.org/en/4.1/pages/configuration/elasticsearch.html

Configuration options: ``elasticsearch_version_probe_attempts`` and ``elasticsearch_version_probe_delay``.

Configuration file changes
--------------------------

The system stats collector has been reimplemented using OSHI instead of SIGAR.
The configuration option `disable_sigar` has been renamed to `disable_native_system_stats_collector`.


Change of API endpoint for user retrieval and modification
----------------------------------------------------------

+-----------------------------------------------+-----------------------------+
| Endpoint                                      | Description                 |
+===============================================+=============================+
| ``PUT /example/placeholder``                  | TODO placeholder comment    |
+-----------------------------------------------+-----------------------------+


API Endpoint Deprecations
=========================

The following API endpoints are deprecated beginning with 4.1.

API Endpoint Removals
=====================

The following API endpoints have been removed in 4.1.

