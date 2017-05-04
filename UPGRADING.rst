**************************
Upgrading to Graylog 2.3.x
**************************

.. _upgrade-from-22-to-23:

This file only contains the upgrade note for the upcoming release.
Please see `our documentation <http://docs.graylog.org/en/latest/pages/upgrade.html>`_
for the complete upgrade notes.

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
