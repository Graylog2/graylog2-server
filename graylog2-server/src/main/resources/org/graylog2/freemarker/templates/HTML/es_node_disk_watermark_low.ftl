<#if _title>Elasticsearch nodes disk usage above low watermark</#if>

<#if _description><span>
There are Elasticsearch nodes in the cluster running out of disk space, their disk usage is above the low watermark.
For this reason Elasticsearch will not allocate new shards to the affected nodes.
The affected nodes are: [${nodes}]
Check <a href="https://www.elastic.co/guide/en/elasticsearch/reference/master/disk-allocator.html" target="_blank" rel="noreferrer">https://www.elastic.co/guide/en/elasticsearch/reference/master/disk-allocator.html</a> for more details.
</span></#if>
