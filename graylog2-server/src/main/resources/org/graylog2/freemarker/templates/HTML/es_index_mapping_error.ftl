<#if _title>Field type conflict in index ${index}</#if>

<#if _description><span>
Messages were rejected because the field <em>${field}</em> is mapped to a numeric type in this index, while Graylog
writes date values for it. Graylog converts these values into the numeric representation that the mapping expects, so
that the messages can still be indexed, but the conflict remains: the type of a field cannot be changed in an existing
index.<br />
To resolve it, change the type of this field to "Date", or rotate the index set so that the next index picks up the
correct type.
</span></#if>
