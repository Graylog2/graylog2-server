<#if _title>${title}</#if>
<#if _description><span>
Please check the following indices as your assistance may be necessary to resolve the issue:
<ul>
    <#list rolloverErrors as error>
    <li>${error}</li>
    </#list>
</ul>
</span></#if>
