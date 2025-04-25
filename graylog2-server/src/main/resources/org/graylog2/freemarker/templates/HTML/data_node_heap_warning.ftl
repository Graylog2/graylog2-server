<#if _title>
    Data Node Heap Size Warning
</#if>

<#if _description>
    <p>
        There are data node nodes in the cluster which could potentially run with a higher configured heap size for better performance.
    </p>
    <p>
        Data node <em>${hostname}</em> only has ${heapSize} Java Heap assigned, out of a total of ${totalMemory} RAM.<br/>
        Currently, there is ${availableMemory} free memory available on the node. We recommend to make an additional half of this available to the Java Heap.
    </p>
    <p>
        <em>Note: </em>For production performance, it is recommended to configure this node to use ${recommendedMemory} Java Heap (50% of RAM).<br/>
        The Java Heap can be configured using the <em>opensearch_heap</em> configuration parameter in the node's configuration file (<em>datanode.conf</em>).
    </p>
</#if>
