**************************
Upgrading to Graylog 2.4.x
**************************

.. _upgrade-from-23-to-24:

This file only contains the upgrade note for the upcoming release.
Please see `our documentation <http://docs.graylog.org/en/latest/pages/upgrade.html>`_
for the complete upgrade notes.

More plugins shipped by default
===============================

The following Graylog plugins are now shipped as part of the Graylog server release.

- AWS Plugin - https://github.com/Graylog2/graylog-plugin-aws
- Threat Intelligence Plugin - https://github.com/Graylog2/graylog-plugin-threatintel
- NetFlow Plugin - https://github.com/Graylog2/graylog-plugin-netflow
- CEF Plugin - https://github.com/Graylog2/graylog-plugin-cef

Make sure you remove all previous versions of these plugins from your ``plugin/`` folder!

Removal of anonymous usage-stats plugin
=======================================

The `anonymous usage-stats plugin <https://github.com/Graylog2/graylog-plugin-anonymous-usage-statistics>`_
got removed from Graylog and is now deprecated. Make sure you remove all old versions
of the plugin from your ``plugin/`` folder!
