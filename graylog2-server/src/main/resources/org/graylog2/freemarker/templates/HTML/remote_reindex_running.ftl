<#if _title>Remote Reindex Migration is running</#if>

<#if _description><span>
    Remote reindexing your existing data into the Graylog data node is running.<br />
    <#if DATA_NODE_MIGRATION_WIZARD?has_content>
        Please visit the <a href="${DATA_NODE_MIGRATION_WIZARD}" target="_blank" rel="noreferrer">data node migration wizard</a> to see the current progress.
    </#if>
    </#if>
