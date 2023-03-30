<#if _title>
Elasticsearch nodes with too low open file limit
</#if>

<#if _description>
There are Elasticsearch nodes in the cluster that have a too low open file limit.
Current limit: ${max_file_descriptors} on ${hostname} (should be at least 64000).
This will be causing problems that can be hard to diagnose. Read how to raise the
maximum number of open files here: https://docs.graylog.org/docs/elasticsearch#configuration
</#if>
