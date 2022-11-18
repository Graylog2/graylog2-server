<#if _title>
An error occurred while trying to send an email!
</#if>

<#if _description>
<span>
The Graylog server encountered an error while trying to send an email.
This is the detailed error message: ${exception}
</span>
</#if>
