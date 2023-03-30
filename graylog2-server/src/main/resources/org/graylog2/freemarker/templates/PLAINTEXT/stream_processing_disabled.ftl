<#if _title>Processing of a stream has been disabled due to excessive processing time</#if>

<#if _description>
The processing of stream ${stream_title} (${stream_id}) has taken too long for ${fault_count} times.
To protect the stability of message processing, this stream has been disabled. Please correct the
stream rules and reenable the stream.
Check here for more details: https://docs.graylog.org/docs/streams#stream-processing-runtime-limits
</#if>
