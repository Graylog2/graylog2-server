<#if _title>
    Data Node Certificate Reload Failed
</#if>

<#if _description>
    <span>
        Data node <em>${node_id}</em> did not pick up its renewed certificate on the Opensearch HTTP layer: ${reason}
    </span>
</#if>
