<#if _title>
Nodes with too long GC pauses
</#if>

<#if _description>
There are Graylog nodes on which the garbage collector runs too long.
Garbage collection runs should be as short as possible. Please check whether those nodes are healthy.
(Node: ${node_id}, GC duration: ${gc_duration_ms} ms,
GC threshold: ${gc_threshold_ms} ms
</#if>
