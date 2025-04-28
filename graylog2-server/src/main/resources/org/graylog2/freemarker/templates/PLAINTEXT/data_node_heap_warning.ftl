<#if _title>
    Data Node Heap Size Warning
</#if>

<#if _description>
    There are data node nodes in the cluster which could potentially run with a higher configured heap size for better performance.
    Data node ${hostname} only has ${heapSize} Java Heap assigned, out of a total of ${totalMemory} RAM.
    Currently, there is ${availableMemory} free memory available on the node. We recommend to make an additional half of this available to the Java Heap.
    Note: For production performance, it is recommended to configure this node to use ${recommendedMemory} Java Heap (50% of RAM).
    The Java Heap can be configured using the opensearch_heap configuration parameter in the node's configuration file (datanode.conf).
</#if>
