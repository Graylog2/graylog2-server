<#if _title>
Indexer cluster unavailable
</#if>

<#if _description>
<span>
We could not successfully connect to the Indexer cluster. If you are using multicast, check that
it is working in your network and that the Indexer is accessible. Also check that the cluster name setting
is correct. Read how to fix this in
<a href="https://docs.graylog.org/docs/elasticsearch#configuration" target="_blank" rel="noreferrer">the setup documentation.</a>
</span>
</#if>
