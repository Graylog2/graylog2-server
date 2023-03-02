<#if _title>
Elasticsearch nodes with too low open file limit
</#if>

<#if _description>
<span>
There are Elasticsearch nodes in the cluster that have a too low open file limit (current limit:
<em>${max_file_descriptors}</em> on <em>${hostname}</em>;
should be at least 64000) This will be causing problems
that can be hard to diagnose. Read how to raise the maximum number of open files in
<a href="https://docs.graylog.org/docs/elasticsearch#configuration" target="_blank" rel="noreferrer">the Elasticsearch setup documentation</a>.
</span>
</#if>
