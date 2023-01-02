<#if _title>
${title}
</#if>

<#if _description>
${description}
<#list blockDetails>
    <#items as line>
    ${line[0]}: ${line[1]}
    </#items>
</#list>
</#if>
