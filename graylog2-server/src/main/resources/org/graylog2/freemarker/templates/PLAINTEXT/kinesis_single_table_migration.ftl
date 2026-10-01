<#if _title>
Kinesis input KCL update
</#if>

<#if _description>
In Graylog 7.2, the AWS Kinesis/CloudWatch input has been updated to Kinesis Client Library (KCL) 3.5. Kinesis inputs created before Graylog 7.2 can now migrate to a single DynamoDB table for state tracking using the new "Migrate to single DynamoDB table for state tracking" option in the Edit input dialog on the Inputs page. Inputs created on Graylog 7.2 or later already use a single table and don't need to be migrated.

Migrating means fewer tables to manage and, for some customers, a lower risk of hitting AWS account limits. The migration is optional today but may be required by AWS in the future, so you can now migrate on your own schedule.

The migration process requires monitoring and cannot be rolled back once complete. Before starting the migration, please review the Kinesis section of the Graylog upgrade notes: https://github.com/Graylog2/graylog2-server/blob/7.2/UPGRADING.md#aws-kinesiscloudwatch-input-single-dynamodb-table-state-tracking
</#if>
