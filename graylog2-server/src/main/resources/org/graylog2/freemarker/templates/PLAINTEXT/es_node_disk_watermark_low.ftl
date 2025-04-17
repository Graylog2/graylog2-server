<#if _title>Indexer nodes disk usage above low watermark</#if>

<#if _description>
There are Indexer nodes in the cluster running out of disk space, their disk usage is above the low watermark.
For this reason, no new shards will be allocated on the affected nodes.
The affected nodes are: [${nodes}]
Check for more details: https://www.elastic.co/guide/en/elasticsearch/reference/master/disk-allocator.html
</#if>
