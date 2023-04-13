<#if _title>Elasticsearch nodes disk usage above flood stage watermark</#if>

<#if _description><span>
There are Elasticsearch nodes in the cluster without free disk, their disk usage is above the flood stage watermark.
For this reason Elasticsearch enforces a read-only index block on all indexes having any of their shards in any of the
affected nodes. The affected nodes are: [${nodes}]
Check <a href="https://www.elastic.co/guide/en/elasticsearch/reference/master/disk-allocator.html" target="_blank" rel="noreferrer">https://www.elastic.co/guide/en/elasticsearch/reference/master/disk-allocator.html</a>
for more details.
    </span></#if>
