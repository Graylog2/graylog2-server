<#if _title>Uncommited messages deleted from journal</#if>

<#if _description><span>
Some messages were deleted from the journal before they could be written to the Indexer. Please
verify that your Indexer cluster is healthy and fast enough. You may also want to review your
journal settings and set a higher limit. (Node: <em>${node_id}</em>)
</span></#if>
