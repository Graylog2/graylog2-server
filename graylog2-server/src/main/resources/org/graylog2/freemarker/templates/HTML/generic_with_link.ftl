<#if _title>
${title}
</#if>

<#if _description>
    <#if GENERIC_DETAILS?has_content>
        ${GENERIC_DETAILS}
    </#if>
    <br>
    <#if GENERIC_URL?has_content>
        You can click <a href="${GENERIC_URL}" target="_blank" rel="noreferrer">here</a> to solve this.
    </#if>
</#if>
