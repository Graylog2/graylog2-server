<#if _title>Processing of a stream has been disabled due to excessive processing time</#if>

<#if _description><span>
The processing of stream <em>${stream_title} (${stream_id})</em> has taken too long for
${fault_count} times. To protect the stability of message processing,
this stream has been disabled. Please correct the stream rules and reenable the stream.
Check <a href="https://docs.graylog.org/docs/streams#stream-processing-runtime-limits" target="_blank" rel="noreferrer">the documentation</a>
for more details.
</span></#if>
