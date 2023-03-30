<#if _title>
Index ranges recalculation required
</#if>

<#if _description>
The index ranges are out of sync. Please go to System/Indices and trigger a index range recalculation from
the Maintenance menu of the following index sets:
    <#if index_sets??>
        ${index_sets}
    <#else>
        all index sets
    </#if>
</#if>

