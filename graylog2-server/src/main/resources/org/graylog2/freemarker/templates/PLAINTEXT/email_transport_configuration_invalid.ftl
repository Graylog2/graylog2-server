<#if _title>
Email Transport Configuration is missing or invalid!
</#if>

<#if _description>
The configuration for the email transport subsystem has shown to be missing or invalid.
Please check the related section of your Graylog server configuration file.
This is the detailed error message: ${exception}
</#if>
