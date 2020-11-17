**************************
Upgrading to Graylog 4.0.x
**************************

.. _upgrade-from-33-to-40:

.. contents:: Overview
   :depth: 3
   :backlinks: top

.. warning:: Please make sure to create a MongoDB database backup before starting the upgrade to Graylog 4.0!

Breaking Changes
================

Changes to the Elasticsearch Support
------------------------------------

Starting with Graylog v4.0, bigger changes to the Elasticsearch versions supported are happening:

  - Support for Elasticsearch versions prior to v6.8.0 is dropped.
  - Support for Elasticsearch v7.x is now included.

This means that you can upgrade to Graylog v4.0 without an Elasticsearch update only if you have been on at least Elasticsearch v6.8.0 before.
Additionally, due to the fact that Elasticsearch supports only indices created by the last two major versions (i.e. ES6.8.0+ reads indices created by ES5 & ES6, while ES7 reads indices created by ES6 & ES7), you can change to Graylog v4.0 with an Elasticsearch update without reindexing only if you have been on at least Elasticsearch v6.0.0 before.
If you have been on any older Elasticsearch version you need to reindex every index you want to keep once for every two major versions of Elasticsearch up to at least ES6.

When upgrading Elasticsearch from one major version to another, please read the upgrade guides provided by elastic:

  - `To 6.8.0 <https://www.elastic.co/guide/en/elasticsearch/reference/6.8/setup-upgrade.html>`_
  - `To 7.9.0 <https://www.elastic.co/guide/en/elasticsearch/reference/7.9/setup-upgrade.html>`_

And our :ref:`Elasticsearch Upgrade Notes <upgrading-elasticsearch>`.

Please do notice that Graylog does not support rolling upgrades between major versions, while Elasticsearch does. If you are upgrading from one major version of Elasticsearch to another, you need to restart Graylog in order to reinitialize the storage module. A procedure which allows a rolling upgrade of Elasticsearch between two major versions of a multi-node Graylog cluster is outlined :ref:`here <es_rolling_upgrade>`.

LDAP and Active Directory configuration changes
-----------------------------------------------

The LDAP and Active Directory authentication and authorization support has been rewritten in Graylog 4.0. Existing LDAP settings will automatically be migrated to the new backend.

Because the new backends require more configuration settings than the old one, migrated backends are **disabled by default**! Please make sure to review the migrated backends under "System / Authentication" and enable one backend to make it possible to login with LDAP or Active Directory users again.

LDAP and Active Directory group mapping removed
-----------------------------------------------

The old LDAP and Active Directory group mapping has been replaced by teams support in Graylog Enterprise.

Existing custom roles will be migrated to the new entity sharing system on the first server startup.

Fixing certificate validation for LDAP servers used for authentication
----------------------------------------------------------------------

Prior to v3.3.3, the certificates of LDAP servers which are connected to using a secure connection (SSL or TLS) were not validated, even if the "Allow self-signed certificates" option was unchecked. Starting with v3.3.3, certificates are validated against the local default keystore. This might introduce a breaking change, depending on your local LDAP settings and the validity of the certificates used (if any). Please ensure that all certificates used are valid, their common name matches the host part of your configured LDAP server and your local keystore contains all CA/intermediate certs required for validation.

A `CVE <https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2020-15813>`_ is tracked for this issue.

Change of API endpoint for user retrieval and modification
----------------------------------------------------------

In 4.0 we changed the following user API endpoints to expect a user ID parameter instead of the username:

+-----------------------------------------------+-----------------------------+
| Endpoint                                      | Description                 |
+===============================================+=============================+
| ``PUT /users/{userId}``                       | Update user account         |
+-----------------------------------------------+-----------------------------+
| ``PUT /users/{userId}/password``              | Update user password        |
+-----------------------------------------------+-----------------------------+
| ``GET /users/{userId}/tokens``                | Get user tokens             |
+-----------------------------------------------+-----------------------------+
| ``POST /users/{userId}/tokens/{name}``        | Generate new user API token |
+-----------------------------------------------+-----------------------------+
| ``DELETE /users/{userId}/tokens/{idOrToken}`` | Delete user API token       |
+-----------------------------------------------+-----------------------------+

Disabled Cross-Origin Requests by Default
-----------------------------------------

For improved security, Cross-Origin requests towards the API server are now disallowed by default.
In the rare case, that your setup is serving the frontend assets from a different
origin than the server, you can re-enable this with ``http_enable_cors = true`` in ``graylog.conf``.

Removal of pluggable authentication realm Java APIs
---------------------------------------------------

The Java API to implement custom authentication realms has been removed and got replaced with with the ``AuthServiceBackend`` Java API.

SSO Authentication Plugin
-------------------------

Due to the aforementioned removal of the pluggable authentication realm Java APIS, the `SSO Authentication Plugin <https://github.com/Graylog2/graylog-plugin-auth-sso>`_ doesn't work with Graylog 4.0 anymore.

The core feature of the old SSO plugin (trusted HTTP header authentication) got integrated in the server.

The old SSO plugin **must be removed** from the plugin folder before starting a Graylog 4.0 server.


API Endpoint Deprecations
=========================

The following API endpoints are deprecated beginning with 4.0.

Deprecation of cluster stats endpoints
--------------------------------------

Starting with v4.0, the cluster stats endpoints are deprecated and will be removed in a future version. Those include:

- ``/system/cluster/stats``
- ``/system/cluster/stats/elasticsearch``
- ``/system/cluster/stats/mongo``

Deprecation of API endpoint for unpaginated listing of grok patterns
--------------------------------------------------------------------

In 3.0 we introduced a new API endpoint to retrieve grok patterns from the backend: ``/system/grok/paginated`` which allows
to pass pagination parameters.
We therefore mark ``/system/grok`` as deprecated. Users who use this endpoint for scripting purpose should change
their scripts to the format of the new endpoint, so they only need to to change the URL when ``/system/grok/paginated`` will become
``/system/grok``.

Deprecation of API endpoint for unpaginated listing of streams
--------------------------------------------------------------

In 4.0 we introduce a new API endpoint to retrieve streams from the backend: ``/streams/paginated`` which allows
to pass pagination parameters.
We therefore mark ``/streams`` as deprecated. Users who use this endpoint for scripting purpose should change
their scripts to the format of the new endpoint, so they only need to to change the URL when ``/streams/paginated`` will become
``/streams``.

Deprecation of API endpoint for unpaginated listing of users
------------------------------------------------------------

In 4.0 we introduce a new API endpoint to retrieve users from the backend: ``/users/paginated`` which allows
to pass pagination parameters.
We therefore mark ``/users`` as deprecated. Users who use this endpoint for scripting purpose should change
their scripts to the format of the new endpoint, so they only need to to change the URL when ``/users/paginated`` will become
``/users``.

API Endpoint Removals
=====================

The following API endpoints have been removed in 4.0.

Removal of legacy Dashboard API
-------------------------------

Starting with 3.3, the previous Dashboard API was replaced by the views API. Therefore it was moved to a ``/legacy`` prefix
and marked for deprecation. Now it is removed altogether.

Removal of legacy Saved Searches API
------------------------------------

Starting with 3.3, the previous Saved Searches API was replaced by the views API. Therefore it was moved to a ``/legacy`` prefix
and marked for deprecation. Now it is removed altogether.

Removal of legacy redirects for Dashboards & Saved Search API
-------------------------------------------------------------

For 3.3, the pre-views Dashboards & Saved Searches APIs were moved to a ``/legacy`` prefix. The new APIs were moved to ``/dashboards`` & ``/search/saved`` and legacy redirects were created for the previous routes (``/views/dashboards`` & ``/views/savedSearches``).

With 4.0, the legacy redirects (which were marked as being deprecated in 3.3) are removed.

Removal of legacy LDAP API endpoints
------------------------------------

The following API endpoints for LDAP configuration management have been removed. They have been replaced with the new ``/system/authentication/services/backends`` API endpoints.

- ``GET /system/ldap/settings``
- ``PUT /system/ldap/settings``
- ``DELETE /system/ldap/settings``
- ``GET /system/ldap/settings/groups``
- ``PUT /system/ldap/settings/groups``
- ``GET /system/ldap/groups``
- ``POST /system/ldap/test``
