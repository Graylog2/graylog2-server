<#if _title>
Nodes with too long GC pauses
</#if>

<#if _description>
<span>
There are Graylog nodes on which the garbage collector runs too long.
Garbage collection runs should be as short as possible. Please check whether those nodes are healthy.
(Node: <em>${node_id}</em>, GC duration: <em>${gc_duration_ms} ms</em>,
GC threshold: <em>${gc_threshold_ms} ms</em>)
</span>
</#if>
