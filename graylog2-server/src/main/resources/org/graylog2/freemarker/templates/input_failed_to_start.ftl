<#if _title>
    An input has failed to start
</#if>

<#if _description>
    <span>
Input ${input_id} has failed to start on node ${node_id} for this reason:
»${reason}«. This means that you are unable to receive any messages from this input.
This is mostly an indication of a misconfiguration or an error.
    <#if _cloud == false>
        <#if SYSTEM_INPUTS?has_content>
        You can click <a href="${SYSTEM_INPUTS}" target="_blank" rel="noreferrer">here</a> to solve this.
        </#if>
    </#if>
</span>
</#if>
