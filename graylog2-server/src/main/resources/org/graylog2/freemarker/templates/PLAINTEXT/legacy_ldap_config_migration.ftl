<#if _title>Legacy LDAP/Active Directory configuration has been migrated to an Authentication Service</#if>

<#if _description>
    <#if AUTHENTICATION_BACKEND?has_content>
The legacy LDAP/Active Directory configuration of this system has been upgraded to a new
authentication service: ${AUTHENTICATION_BACKEND}
Since the new authentication service requires some information that is not present in the legacy
configuration, it requires a manual review!

After reviewing ${AUTHENTICATION_BACKEND} it must be enabled to allow LDAP or Active Directory users
to log in again!
    <#else>
The legacy LDAP/Active Directory configuration of this system has been upgraded to a new authentication service<.
Since the new authentication service requires some information that is not present in the legacy
configuration, it requires a manual review!

After reviewing the authentication service it must be enabled to allow LDAP or Active Directory users to log in again!
    </#if>
Please check the upgrade guide for more details: https://docs.graylog.org/docs/upgrading-graylog
</#if>
