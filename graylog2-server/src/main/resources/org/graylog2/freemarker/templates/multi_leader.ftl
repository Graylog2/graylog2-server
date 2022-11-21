<#if _title>Multiple Graylog server leaders in the cluster</#if>

<#if _description><span>
There were multiple Graylog server instances configured as leader in your Graylog cluster. The cluster handles
this automatically by launching new nodes as followers if there already is a leader but you should still fix this.
Check the graylog.conf of every node and make sure that only one instance has  <code>is_leader = true</code>. Close this
notification if you think you resolved the problem. It will pop back up if you start a second leader node again.
</span></#if>
