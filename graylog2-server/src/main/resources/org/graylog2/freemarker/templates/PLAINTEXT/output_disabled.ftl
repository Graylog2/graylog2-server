<#if _title>Output disabled</#if>

<#if _description>
The output with the id ${outputId} in stream &quot;${streamTitle}&quot;
(id: ${streamId}) has been disabled for ${faultPenaltySeconds}
seconds because there were ${faultCount} failures.
(Node: ${node_id}, Fault threshold: ${faultCountThreshold})
</#if>
