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

