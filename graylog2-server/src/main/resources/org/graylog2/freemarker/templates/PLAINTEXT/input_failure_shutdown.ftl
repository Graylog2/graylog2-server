<#if _title>
An input has shut down due to failures
</#if>

<#if _description>
Input ${input_title} has shut down on node ${node_id} for this reason:
   »${reason}«
This means that you are unable to receive any messages from this input.
This is often an indication of persistent network failures.
        <#if _cloud == false>
            <#if SYSTEM_INPUTS?has_content>
You can click here to see the input: ${SYSTEM_INPUTS}
            </#if>
        </#if>
</#if>

