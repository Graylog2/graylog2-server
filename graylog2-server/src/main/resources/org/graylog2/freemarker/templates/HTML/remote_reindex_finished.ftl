<#if _title>Remote Reindex Migration has finished</#if>

<#if _description><span>
    Remote reindexing your existing data into the Graylog data node has finished <#if status == 'FINISHED'>sucessfully<#else>with errors</#if>.<br />
    <#if DATA_NODE_MIGRATION_WIZARD?has_content>
        Please visit the <a href="${DATA_NODE_MIGRATION_WIZARD}" target="_blank" rel="noreferrer">data node migration wizard</a> to finalize the migration.
    </#if>
    </#if>
