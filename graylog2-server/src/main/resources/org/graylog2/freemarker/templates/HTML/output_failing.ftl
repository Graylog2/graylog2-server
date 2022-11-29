<#if _title>Output failing</#if>

<#if _description><span>
The output &quot;${outputTitle}&quot; (id: ${outputId})
in stream &quot;${streamTitle}&quot; (id: ${streamId})
is unable to send messages to the configured destination.
<br />
The error message from the output is: <em>${errorMessage}</em>
</span></#if>
