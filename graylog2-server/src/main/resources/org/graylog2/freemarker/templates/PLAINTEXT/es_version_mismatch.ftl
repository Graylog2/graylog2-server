<#if _title>Indexer version is incompatible</#if>

<#if _description><
The Indexer version which is currently running (${current_version}) has a different major version than
the one the leader node was started with (${initial_version}).
This will most probably result in errors during indexing or searching. A full restart is required after a major
Indexer version upgrade.
For details, please see our notes here: "https://docs.graylog.org/docs/rolling-es-upgrade
</#if>

