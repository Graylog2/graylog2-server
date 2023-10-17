<#if _title>
    A data node needs provisioning
</#if>

<#if _description>
    <span>
A data node has recently been started for the first time and is waiting for admission to the cluster and certificate provisioning.

Due to the configured manual certificate renewal policy, a manual action is required for the provisioning of this node.

<p />

    <#if _cloud == false>
        <#if DATA_NODE_CONFIGURATION?has_content>
        You can click <a href="${DATA_NODE_CONFIGURATION}" target="_blank" rel="noreferrer">here</a> to solve this.
        </#if>
    </#if>
</span>
</#if>
