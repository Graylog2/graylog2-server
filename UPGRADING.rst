**************************
Upgrading to Graylog 4.2.x
**************************

.. _upgrade-from-41-to-42:

.. contents:: Overview
   :depth: 3
   :backlinks: top

.. warning:: Please make sure to create a MongoDB database backup before starting the upgrade to Graylog 4.2!

Breaking Changes
================

Search From/To by Keyword
-------------------------
Prior to this version, if the time was inferred from the keyword string (e.g. "last week" or "last monday"),
the interval did not make much sense, because the hour/minute/sec part of the interval was taken from the moment
in time, the query was submitted. So, the intervals were not aligned to something that made sense.
This has been changed so that. e.g. "last monday" is indeed aligned to start at 00:00:00 and ends on the next day at 00:00:00.
Also, ending on the next day at 00:00:00 is a breaking change. This was chosen so that millis/nanos etc. until the very end
of the interval are included in the search (and not because of different messages with handling of millis, nanos etc. some messages
get omitted).

Changes to the Elasticsearch Support
------------------------------------

Configuration file changes
--------------------------

Change of API endpoint for user retrieval and modification
----------------------------------------------------------

+-----------------------------------------------+-----------------------------+
| Endpoint                                      | Description                 |
+===============================================+=============================+
| ``PUT /example/placeholder``                  | TODO placeholder comment    |
+-----------------------------------------------+-----------------------------+


API Endpoint Deprecations
=========================

The following API endpoints are deprecated beginning with 4.2.

API Endpoint Removals
=====================

The following API endpoints have been removed in 4.2.

