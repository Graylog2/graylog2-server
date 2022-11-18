<#if _title>
Deflector exists as an index and is not an alias
</#if>

<#if _description>
<span>
The deflector is meant to be an alias but exists as an index. Multiple failures of infrastructure can lead
to this. Your messages are still indexed but searches and all maintenance tasks will fail or produce incorrect
results. It is strongly recommend that you act as soon as possible.
</span>
</#if>
