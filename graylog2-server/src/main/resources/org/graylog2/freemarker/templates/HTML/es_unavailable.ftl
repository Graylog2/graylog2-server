<#if _title>
Elasticsearch cluster unavailable
</#if>

<#if _description>
<span>
Graylog could not successfully connect to the Elasticsearch cluster. If you are using multicast, check that
it is working in your network and that Elasticsearch is accessible. Also check that the cluster name setting
is correct. Read how to fix this in
<a href="https://docs.graylog.org/docs/elasticsearch#configuration" target="_blank" rel="noreferrer">the Elasticsearch setup documentation.</a>
</span>
</#if>
