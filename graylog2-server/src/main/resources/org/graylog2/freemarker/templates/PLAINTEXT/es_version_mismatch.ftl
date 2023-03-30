<#if _title>Elasticsearch version is incompatible</#if>

<#if _description><
The Elasticsearch version which is currently running (${current_version}) has a different major version than
the one the Graylog leader node was started with (${initial_version}).
This will most probably result in errors during indexing or searching. Graylog requires a full restart after an
Elasticsearch upgrade from one major version to another.
For details, please see our notes here: "https://docs.graylog.org/docs/rolling-es-upgrade
</#if>

