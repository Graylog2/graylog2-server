<#if _title>
Elasticsearch cluster unhealthy (RED)
</#if>

<#if _description>
<span>
The Elasticsearch cluster state is RED which means shards are unassigned.
This usually indicates a crashed and corrupt cluster and needs to be investigated. Graylog will write
into the local disk journal. Read how to fix this in
<a href="https://docs.graylog.org/docs/elasticsearch#cluster-status-explained" target="_blank" rel="noreferrer">the Elasticsearch setup documentation</a>
</span>
</#if>
