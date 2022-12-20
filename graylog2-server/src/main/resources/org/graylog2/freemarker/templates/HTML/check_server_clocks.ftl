<#if _title>
Check the system clocks of your Graylog server nodes
</#if>

<#if _description>
<span>
A Graylog server node detected a condition where it was deemed to be inactive immediately after being active.
This usually indicates either a significant jump in system time, e.g. via NTP, or that a second Graylog server node
is active on a system that has a different system time. Please make sure that the clocks are synchronized.
</span>
</#if>
