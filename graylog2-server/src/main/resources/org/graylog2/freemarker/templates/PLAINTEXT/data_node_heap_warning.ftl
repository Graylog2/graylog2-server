<#if _title>
    Data Node Heap Size Warning
</#if>

<#if _description>
    There are data node nodes in the cluster which could potentially run with a higher configured heap size for better performance.
    On ${hostname}, the free available memory is about ${memoryRatio} times higher than the configured heap size for OpenSearch.
    If this is intentional, you can disregard this warning.
</#if>
