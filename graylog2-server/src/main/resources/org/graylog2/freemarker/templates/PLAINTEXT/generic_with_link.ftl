<#if _title>
${title}
</#if>

<#if _description>
    <#if GENERIC_DETAILS?has_content>
        ${GENERIC_DETAILS}
    </#if>
    <br>
    <#if GENERIC_URL?has_content>
        You can click here to solve this: ${GENERIC_URL}
    </#if>
</#if>
