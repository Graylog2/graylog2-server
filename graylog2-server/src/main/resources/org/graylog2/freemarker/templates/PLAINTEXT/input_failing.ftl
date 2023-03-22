<#if _title>
An input has failed
</#if>

<#if _description>
Input ${input_id} is failing on node ${node_id} for this reason:
   »${reason}«.
This means that you are unable to receive any messages from this input.
This is mostly an indication of a misconfiguration or an error.
    <#if _cloud == false>
        <#if SYSTEM_INPUTS?has_content>
Click here to solve this: ${SYSTEM_INPUTS}
        </#if>
    </#if>
</#if>
