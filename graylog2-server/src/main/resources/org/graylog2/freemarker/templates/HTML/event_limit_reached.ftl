<#if _title>
    Event limit reached
</#if>

<#if _description>
    <span>
        Event limit »${event_limit}« reached for event definition »${event_definition_title}(${event_definition_id})«. Try to use a more specific search query or use aggregations. Otherwise try to raise the limit.
    </span>
</#if>
