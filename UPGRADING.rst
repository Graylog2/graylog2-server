**************************
Upgrading to Graylog 4.0.x
**************************

.. _upgrade-from-33-to-40:

Deprecation of cluster stats endpoints
======================================

Starting with v4.0, the cluster stats endpoints are deprecated and will be removed in a future version. Those include:

  - '/system/cluster/stats'
  - '/system/cluster/stats/elasticsearch'
  - '/system/cluster/stats/mongo'

[BREAKING] Fixing certificate validation for LDAP servers used for authentication
=================================================================================

Prior to v3.3.3, the certificates of LDAP servers which are connected to using a secure connection (SSL or TLS) were not validated, even if the "Allow self-signed certificates" option was unchecked. Starting with v3.3.3, certificates are validated against the local default keystore. This might introduce a breaking change, depending on your local LDAP settings and the validity of the certificates used (if any). Please ensure that all certificates used are valid, their common name matches the host part of your configured LDAP server and your local keystore contains all CA/intermediate certs required for validation.

A `CVE <https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2020-15813>`_ is tracked for this issue.

Change of API endpoint for user retrieval and modification
==========================================================

In 4.0 we changed most of the user API endpoint `/users` to expect user IDs instead of names.

Deprecation of API endpoint for unpaginated listing of grok patterns
====================================================================

In 3.0 we introduce a new API endpoint to retrieve grok patterns from the backend: '/system/grok/paginated' which allows
to pass pagination parameters.
We therefore mark '/system/grok' as deprecated. Users who use this endpoint for scripting purpose should change
their scripts to the format of the new endpoint, so they only need to to change the URL when '/system/grok/paginated' will become
'/system/grok'.


Deprecation of API endpoint for unpaginated listing of streams
==============================================================

In 4.0 we introduce a new API endpoint to retrieve streams from the backend: '/streams/paginated' which allows
to pass pagination parameters.
We therefore mark '/streams' as deprecated. Users who use this endpoint for scripting purpose should change
their scripts to the format of the new endpoint, so they only need to to change the URL when '/streams/paginated' will become
'/streams'.

Deprecation of API endpoint for unpaginated listing of users
==============================================================

In 4.0 we introduce a new API endpoint to retrieve users from the backend: '/users/paginated' which allows
to pass pagination parameters.
We therefore mark '/users' as deprecated. Users who use this endpoint for scripting purpose should change
their scripts to the format of the new endpoint, so they only need to to change the URL when '/users/paginated' will become
'/users'.

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


