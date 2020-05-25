**************************
Upgrading to Graylog 4.0.x
**************************

.. _upgrade-from-33-to-40:

Removal of legacy Saved Searches API
====================================

Starting with `3.3.0`, the pre-views Saved Searches API was replaced, moved to a `/legacy` prefix and marked for [deprecation](https://github.com/Graylog2/graylog2-server/blob/3.3.0/UPGRADING.rst#saved-searches-api).

This PR is removing it altogether, as announced in the upgrade notes for 3.3.

Removal of legacy redirects for Dashboards & Saved Search API
=============================================================

For 3.3, the pre-views Dashboards & Saved Searches APIs were moved to a ``/legacy`` prefix. The new APIs were moved to ``/dashboards`` & ``/search/saved`` and legacy redirects were created for the previous routes (``/views/dashboards`` & ``/views/savedSearches``).

With 4.0, the legacy redirects (which were marked as being deprecated in 3.3) are removed.

Disable Cross-Origin Requests by Default
========================================

For improved security, Cross-Origin requests towards the API server are now disallowed by default.
In the rare case, that your setup is serving the frontend assets from a different
origin than the server, you can reenable this by with ``http_enable_cors = true`` in ``graylog.conf``.


