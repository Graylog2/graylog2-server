<#if _title>
An error occurred while trying to send an email!
</#if>

<#if _description>
<span>
An error was encountered while trying to send an email.
This is the detailed error message: ${exception}
</span>
</#if>
