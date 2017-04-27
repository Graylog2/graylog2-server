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

The deprecated HTTP resources at ``/system/indices/rotation`` and ``/system/indices/retention``, which didn't work since Graylog 2.2.0, have been removed.

These settings are part of the index set configuration and can be configured under ``/system/indices/index_sets``.