*****************
Upgrading Graylog
*****************

.. _upgrade-from-21-to-22:

This file only contains the upgrade note for the upcoming release.
Please see `our documentation <http://docs.graylog.org/en/latest/pages/upgrade.html>`_
for the complete upgrade notes.

From 2.1 to 2.2
===============

Email Alarm Callback
--------------------

In previous Graylog versions, creating an alert condition on a stream and adding an alert receiver meant that, if no alarm callback existed for that stream, Graylog would use an Email alarm callback by default.

Due to the extensive rework done in alerting, this behaviour has been modified to be explicit, and more consistent with other entities within Graylog: from now on **there will not be a default alarm callback**.

To easy the transition to people relying on this behaviour, we have added a migration step that will create an Email alarm callback for each stream that has alert conditions, has alert receivers, but has no associated alarm callbacks.

Default stream/Index Sets
-------------------------

With the introduction of index sets, and the ability to change a stream's write target, the default stream needs additional information, which is calculated when starting a new Graylog 2.2 master node.

It requires recalculation of the index ranges of the default stream's index set, which when updating from pre-2.2 versions is stored in the `graylog_` index. This is potentially expensive, because it has to calculate three aggregations across every open index to detect which streams are stored in which index.

Please be advised that this necessary migration can put additional load on your cluster.

.. warning:: Make sure that all rotation and retention strategy plugins you had installed in 2.1 are updated to a version that is compatible with 2.2 before you start the Graylog 2.2 version for the first time. (e.g. Graylog Enterprise) This is needed so the required data migrations will run without problems.

RotationStrategy & RetentionStrategy Interfaces
-----------------------------------------------

The Java interfaces for ``RetentionStrategy`` and ``RotationStrategy`` changed in 2.2. The ``#rotate()`` and ``#retain()`` methods are now getting an ``IndexSet`` as first parameter.

This only affects you if you are using custom rotation or retention strategies.

Changes in Exposed Configuration
--------------------------------

The exposed configuration settings on the ``/system/configuration`` resource of the Graylog REST API doesn't contain the following (deprecated) Elasticsearch-related settings anymore:

* ``elasticsearch_shards``
* ``elasticsearch_replicas``
* ``index_optimization_max_num_segments``
* ``disable_index_optimization``
