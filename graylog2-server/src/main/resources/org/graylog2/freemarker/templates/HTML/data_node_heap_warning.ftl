<#if _title>
    Data Node Heap Size Warning
</#if>

<#if _description>
    <span>
There are data node nodes in the cluster which could potentially run with a higher configured heap size for better performance.
On <em>${hostname}</em>, the free available memory is about <em>${memoryRatio}</em> times higher than the configured heap size for OpenSearch.
It is recommended to set the heap size to about half the size of your system memory, up to a maximum of 31GB.
If this is intentional, you can disregard this warning.
</span>
</#if>
