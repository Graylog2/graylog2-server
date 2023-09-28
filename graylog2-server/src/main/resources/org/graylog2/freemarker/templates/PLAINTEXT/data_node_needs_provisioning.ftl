<#if _title>
    A data node needs provisioning
</#if>

<#if _description>
A data node has recently been started for the first time and is waiting for admission to the cluster and certificate provisioning.

Due to the configured manual certificate renewal policy, a manual action is required for the provisioning of this node.

    <#if _cloud == false>
        <#if DATA_NODE_CONFIGURATION?has_content>
Click here to solve this: ${DATA_NODE_CONFIGURATION}
        </#if>
    </#if>
</#if>

