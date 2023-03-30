<#if _title>Output disabled</#if>

<#if _description>
<span>
The output with the id ${outputId} in stream &quot;${streamTitle}&quot;
(id: ${streamId}) has been disabled for ${faultPenaltySeconds}
seconds because there were ${faultCount} failures.
(Node: <em>${node_id}</em>, Fault threshold: <em>${faultCountThreshold}</em>)
</span></#if>
