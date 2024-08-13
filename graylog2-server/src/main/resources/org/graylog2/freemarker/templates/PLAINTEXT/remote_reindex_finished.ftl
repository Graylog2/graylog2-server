<#if _title>Remote Reindex Migration has finished</#if>

<#if _description><span>
    Remote reindexing your existing data into the Graylog data node has finished <#if status == 'FINISHED'>sucessfully<#else>with errors</#if>.
    Please visit the data node migration wizard to finalize the migration.
</#if>
