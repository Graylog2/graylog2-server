<#if _title>Elasticsearch nodes disk usage above low watermark</#if>

<#if _description>
There are Elasticsearch nodes in the cluster running out of disk space, their disk usage is above the low watermark.
For this reason Elasticsearch will not allocate new shards to the affected nodes.
The affected nodes are: [${nodes}]
Check for more details: https://www.elastic.co/guide/en/elasticsearch/reference/master/disk-allocator.html
</#if>
