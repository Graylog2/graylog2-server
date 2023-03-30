<#if _title>
${title}
</#if>

<#if _description>
<span>
${description}<br />
<#list blockDetails>
    <ul>
    <#items as line>
        <li>${line[0]}: ${line[1]}</li>
    </#items>
    </ul>
</#list>
</span>
</#if>
