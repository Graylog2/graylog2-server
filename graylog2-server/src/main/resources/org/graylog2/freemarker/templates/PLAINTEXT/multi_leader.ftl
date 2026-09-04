<#if _title>Multiple leader nodes in the cluster</#if>

<#if _description>
There were multiple server instances configured as leader in your cluster. The cluster handles
this automatically by launching new nodes as followers if there already is a leader but you should still fix this.
Check the config file of every node and make sure that only one instance has is_leader = true. Close this
notification if you think you resolved the problem. It will pop back up if you start a second leader node again.
</#if>
