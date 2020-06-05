**************************
Upgrading to Graylog 4.0.x
**************************

.. _upgrade-from-33-to-40:

Deprecation of API endpoint for unpaginated listing of streams
==============================================================

In 4.0 we introduce a new API endpoint to retrieve streams from the backend: '/streams/paginated' which allows
to pass pagination parameters.
We therefore mark '/streams' as deprecated. User which use this endpoint for scripting purpose should change
their scripts to the new endpoint, so they only need to to change the URL when '/streams/paginated' will become
'/streams'.

Removal of legacy Dashboard API
===============================

Starting with 3.3, the previous Dashboard API was replaced by the views API. Therefore it was moved to a ``/legacy`` prefix
and marked for deprecation. Now it is removed altogether.

Removal of legacy Saved Searches API
====================================

Starting with 3.3, the previous Saved Searches API was replaced by the views API. Therefore it was moved to a ``/legacy`` prefix
and marked for deprecation. Now it is removed altogether.

Removal of legacy redirects for Dashboards & Saved Search API
=============================================================

For 3.3, the pre-views Dashboards & Saved Searches APIs were moved to a ``/legacy`` prefix. The new APIs were moved to ``/dashboards`` & ``/search/saved`` and legacy redirects were created for the previous routes (``/views/dashboards`` & ``/views/savedSearches``).

With 4.0, the legacy redirects (which were marked as being deprecated in 3.3) are removed.

Disable Cross-Origin Requests by Default
========================================

For improved security, Cross-Origin requests towards the API server are now disallowed by default.
In the rare case, that your setup is serving the frontend assets from a different
origin than the server, you can reenable this by with ``http_enable_cors = true`` in ``graylog.conf``.


