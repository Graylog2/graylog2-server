<#if _title>
    Data Node Heap Size Warning
</#if>

<#if _description>
    <p>
        There are Data Nodes in the cluster which could potentially run with a higher configured heap size for better performance.
    </p>
    <p>
        Data node <em>${hostname}</em> only has ${heapSize} Java Heap assigned, out of a total of ${totalMemory} RAM.<br/>
        We recommend to assign half of memory to Java Heap. For this production performance, it is recommended to configure this node to use ${recommendedMemory} of Java Heap (50% of Ram).
    </p>
    <p>
        The Data Node service is a wrapper for an Opensearch service, and its the Opensearch service which requires the configuration change.<br/>
        The configuration that should be updated is the <b>opensearch_heap</b> property, <#if recommendedMemorySetting?has_content> set to <b>${recommendedMemorySetting}</b> value,</#if> which can be found in the (<em>datanode.conf</em>) file on each Data Node.
    </p>
    <p>
        Note that the Data Node service will need to be restarted for configuration changes to take effect.
    </p>
</#if>
