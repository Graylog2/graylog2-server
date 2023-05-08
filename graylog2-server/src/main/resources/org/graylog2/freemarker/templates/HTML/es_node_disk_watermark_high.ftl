<#if _title>Elasticsearch nodes disk usage above high watermark</#if>

<#if _description><span>
There are Elasticsearch nodes in the cluster with almost no free disk, their disk usage is above the high watermark.
For this reason Elasticsearch will attempt to relocate shards away from the affected nodes.
The affected nodes are: [${nodes}]
Check <a href="https://www.elastic.co/guide/en/elasticsearch/reference/master/disk-allocator.html" target="_blank" rel="noreferrer">https://www.elastic.co/guide/en/elasticsearch/reference/master/disk-allocator.html</a>
for more details.
</span></#if>
