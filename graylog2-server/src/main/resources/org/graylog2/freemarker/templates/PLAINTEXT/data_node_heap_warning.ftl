<#if _title>
    Data Node Heap Size Warning
</#if>

<#if _description>
    There are Data Nodes in the cluster which could potentially run with a higher configured heap size for better performance.
    Data node ${hostname} only has ${heapSize} Java Heap assigned, out of a total of ${totalMemory} RAM.
    We recommend to assign half of memory to Java Heap. For this production performance, it is recommended to configure this node to use ${recommendedMemory} of Java Heap (50% of Ram).
    The Data Node service is a wrapper for an Opensearch service, and its the Opensearch service which requires the configuration change.
    The configuration that should be updated is the opensearch_heap property, <#if recommendedMemorySetting?has_content> set to ${recommendedMemorySetting} value,</#if> which can be found in the (datanode.conf) file on each Data Node.
    Note that the Data Node service will need to be restarted for configuration changes to take effect.
</#if>
