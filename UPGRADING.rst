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


