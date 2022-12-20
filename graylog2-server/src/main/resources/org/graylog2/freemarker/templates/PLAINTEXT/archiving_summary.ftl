<#if _title>Indices could not be archived yet</#if>

<#if _description>
There was an error while archiving some indices. Graylog will continue trying to archive those
indices and will retain all indices until they are successfully archived.
Please check the following error messages as your assistance may be necessary to resolve the issue:
    <#list archiveErrors as error>
    ${error}
    </#list>
</#if>
