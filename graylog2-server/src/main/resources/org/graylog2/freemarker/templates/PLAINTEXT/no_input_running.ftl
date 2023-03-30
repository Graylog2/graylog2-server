<#if _title>
There is a node without any running inputs
</#if>

<#if _description>
There is a node without any running inputs. This means that you are not receiving any messages from this
node at this point in time. This is most probably an indication of an error or misconfiguration.
    <#if _cloud == false>
        <#if SYSTEM_INPUTS?has_content>
         You can click here to solve this: ${SYSTEM_INPUTS}
        </#if>
    </#if>
</#if>
