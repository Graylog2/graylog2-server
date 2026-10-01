<#if _title>
Kinesis input KCL update
</#if>

<#if _description>
<span>
In Graylog 7.2, the AWS Kinesis/CloudWatch input has been updated to Kinesis Client Library (KCL) 3.5. Existing
Kinesis inputs can now migrate to a single DynamoDB table for state tracking using the new
<strong>Migrate to single DynamoDB table for state tracking</strong> option in the Edit input dialog on the Inputs
page.
<br /><br />
Migrating means fewer tables to manage and, for some customers, a lower risk of hitting AWS account limits. The
migration is optional today but may be required by AWS in the future, so you can now migrate on your own schedule.
<br /><br />
The migration process requires monitoring and cannot be rolled back once complete. Before starting the migration,
please review the Kinesis section of the
<a href="https://github.com/Graylog2/graylog2-server/blob/7.2/UPGRADING.md#aws-kinesiscloudwatch-input-single-dynamodb-table-state-tracking" target="_blank" rel="noreferrer">Graylog upgrade notes</a>.
</span>
</#if>
