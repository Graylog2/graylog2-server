<#if _title>Elasticsearch nodes disk usage above flood stage watermark</#if>

<#if _description>
There are Elasticsearch nodes in the cluster without free disk, their disk usage is above the flood stage watermark.
For this reason Elasticsearch enforces a read-only index block on all indexes having any of their shards in any of the
affected nodes. The affected nodes are: [${nodes}]
Check here for more details:"https://www.elastic.co/guide/en/elasticsearch/reference/master/disk-allocator.html
</#if>
