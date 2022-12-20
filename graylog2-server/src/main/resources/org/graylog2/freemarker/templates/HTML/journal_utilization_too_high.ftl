<#if _title>Journal utilization is too high</#if>

<#if _description><span>
Journal utilization is too high and may go over the limit soon. Please verify that your Elasticsearch cluster
is healthy and fast enough. You may also want to review your Graylog journal settings and set a higher limit.
(Node: <em>${node_id}</em>)
</span></#if>

