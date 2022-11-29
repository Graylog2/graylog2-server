<#if _title>Legacy LDAP/Active Directory configuration has been migrated to an Authentication Service</#if>

<#if _description><span>
    <#if AUTHENTICATION_BACKEND?has_content>
The legacy LDAP/Active Directory configuration of this system has been upgraded to a new
<a href="${AUTHENTICATION_BACKEND}" authentication service</a>.
Since the new authentication service requires some information that is not present in the legacy
configuration, it <strong>requires a manual review</strong>!
<br /> <br />
<strong>After reviewing the <a href="${AUTHENTICATION_BACKEND}" authentication service</a> it must be enabled to allow LDAP or Active Directory users
to log in again!
</strong>
    <#else>
The legacy LDAP/Active Directory configuration of this system has been upgraded to a new authentication service<.
Since the new authentication service requires some information that is not present in the legacy
configuration, it <strong>requires a manual review</strong>!
<br /> <br />
        <strong>After reviewing the authentication service it must be enabled to allow LDAP or Active Directory users to log in again!
</strong>
    </#if>
<br />
<br />
Please check the <a href="https://docs.graylog.org/docs/upgrading-graylog">upgrade guide</a>
for more details.
</span></#if>
