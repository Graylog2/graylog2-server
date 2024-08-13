<#if _title>${title}</#if>
<#if _description>
    Please check the following indices as your assistance may be necessary to resolve the issue:
    <#list rolloverErrors as error>
    ${error}
    </#list>
</#if>
