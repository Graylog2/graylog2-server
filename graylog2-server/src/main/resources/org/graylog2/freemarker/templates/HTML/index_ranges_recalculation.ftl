<#if _title>
    Index ranges recalculation required
</#if>

<#if _description>
    <span>
The index ranges are out of sync. Please go to System/Indices and trigger a index range recalculation from
the Maintenance menu of
        <#if index_sets??>
            the following index sets: ${index_sets}
        <#else>all index sets
        </#if>
    </span>
</#if>

