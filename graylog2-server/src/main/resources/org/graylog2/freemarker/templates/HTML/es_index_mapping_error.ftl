<#if _title>Field type conflict in index ${index}</#if>

<#if _description><span>
Messages were rejected because the following fields are mapped to a numeric type in this index, while Graylog writes
date values for them: [${fields}]. Graylog writes these values as epoch milliseconds so that the messages can still be
indexed, but the conflict remains: the type of a field cannot be changed in an existing index.<br />
To resolve it, set a custom field mapping of type "String" for these fields, or rotate the index set so that the next
index picks up the correct type.<br />
Rejected messages so far: ${rejectedMessages}
</span></#if>
