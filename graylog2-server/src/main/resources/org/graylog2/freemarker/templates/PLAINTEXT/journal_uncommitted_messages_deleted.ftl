<#if _title>Uncommited messages deleted from journal</#if>

<#if _description>
Some messages were deleted from the Graylog journal before they could be written to Elasticsearch. Please
verify that your Elasticsearch cluster is healthy and fast enough. You may also want to review your Graylog
journal settings and set a higher limit. (Node: ${node_id})
</#if>
