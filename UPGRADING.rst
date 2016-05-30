*****************
Upgrading Graylog
*****************

.. _upgrade-from-20-to-21:

This file only contains the upgrade note for the upcoming release.
Please see `our documentation <http://docs.graylog.org/en/latest/pages/upgrade.html>`_
for the complete upgrade notes.

From 2.0 to 2.1
===============

For Plugin Authors
------------------

Between 2.0 and 2.1 we also made changes to the Plugin API. These include:

* Removing ``org.graylog2.plugin.streams.Stream#getAlertCondition``, as it was faulty and not easily replaceable with a working version without breaking our separation of models and persistence services.

If you are maintaining a plugin that was originally written for 1.x or 2.0, you need to make sure that your plugin is still compiling and working under 2.1 or adapt it if necessary.
