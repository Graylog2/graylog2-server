<#if _title>Indexer version is incompatible</#if>

<#if _description><span>
The Indexer version which is currently running (${current_version}) has a different major version than
the one the leader node was started with (${initial_version}).
This will most probably result in errors during indexing or searching. A full restart is required after a major
Indexer version upgrade.
<br />
For details, please see our notes about
<a href="https://docs.graylog.org/docs/rolling-es-upgrade" target="_blank" rel="noreferrer">rolling Indexer upgrades.</a>
</span></#if>

