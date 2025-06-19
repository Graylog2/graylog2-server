<#if _title>
Indexer cluster unhealthy (RED)
</#if>

<#if _description>
<span>
The Indexer cluster state is RED which means shards are unassigned.
This usually indicates a crashed and corrupt cluster and needs to be investigated. Messages will be written
into the local disk journal. Read how to fix this
<a href="https://docs.graylog.org/docs/elasticsearch#cluster-status-explained" target="_blank" rel="noreferrer">here</a>
</span>
</#if>
