<#if _title>
Kinesis input KCL update
</#if>

<#if _description>
<span>
In Graylog 7.2, the AWS Kinesis/CloudWatch input has been updated to Kinesis Client Library (KCL) 3.5. Existing
Kinesis inputs can now migrate to a single DynamoDB table for state tracking using the new
<em>Migrate to single DynamoDB table for state tracking</em> option in the Edit Input dialog on the
<#if SYSTEM_INPUTS?has_content><a href="${SYSTEM_INPUTS}" target="_blank" rel="noreferrer">Inputs page</a><#else>Inputs page</#if>.
<br /><br />
Migrating means fewer tables to manage and, for some customers, a lower risk of hitting AWS account limits. The
migration is optional today but may be required by AWS in the future, so you can now migrate on your own schedule.
<br /><br />
The migration process requires monitoring and cannot be rolled back once complete. Before starting the migration,
please review the Kinesis section of the
<a href="https://go2docs.graylog.org/current/upgrading_graylog/upgrade_to_graylog_7.2.htm#aws-kinesis-cloudwatch-input-single-dynamodb-table-state-tracking" target="_blank" rel="noreferrer">Graylog upgrade notes</a>.
</span>
</#if>
