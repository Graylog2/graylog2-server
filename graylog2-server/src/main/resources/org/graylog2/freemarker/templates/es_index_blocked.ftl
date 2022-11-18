<#if _title>
${title}
</#if>

<#if _description>
<span>
${description}<br />
<#list blockDetails>
    <ul>
    <#items as line1, line2>
        <li>{line1}: ${line2}</li>
    </#items>
    </ul>
</#list>
</span>
</#if>
