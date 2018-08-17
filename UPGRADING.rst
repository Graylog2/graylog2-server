**************************
Upgrading to Graylog 2.5.x
**************************

.. _upgrade-from-24-to-25:

This file only contains the upgrade note for the upcoming release.
Please see `our documentation <http://docs.graylog.org/en/latest/pages/upgrade.html>`_
for the complete upgrade notes.

Protecting against CSRF, HTTP header required
=============================================

Graylog server now requires all clients sending non-GET requests against the API to include a custom HTTP header
(``X-Requested-By``). The value of the header is not important, but it's presence is, as all requests without it will
be ignored and will return a 400 error.

**This is important for people using scripts that modify Graylog in any way through the REST API**. We already adapted
Graylog web interface and our plugins, so if you don't use any scripts or 3rd party products to access Graylog, you
don't have to do anything else.
