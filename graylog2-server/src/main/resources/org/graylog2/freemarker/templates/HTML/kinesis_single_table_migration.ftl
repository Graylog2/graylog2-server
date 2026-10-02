<#if _title>
Kinesis input KCL version update
</#if>

<#if _description>
<span>
In ${product_name} 7.2, the AWS Kinesis/CloudWatch input has been updated to Kinesis Client Library (KCL) 3.5.
Kinesis inputs created before ${product_name} 7.2 can now migrate to a single DynamoDB table for state tracking using
the new <strong>Migrate to single DynamoDB table for state tracking</strong> option in the Edit input dialog on the
Inputs page. Inputs created on ${product_name} 7.2 or later already use a single table and don't need to be migrated.
<br /><br />
Migrating means fewer tables to manage and, for some customers, a lower risk of hitting AWS account limits. The
migration is optional today but may be required by AWS in the future, so you can now migrate on your own schedule.
<br /><br />
The migration process requires monitoring and cannot be rolled back once complete. Before starting the migration,
please review the ${product_name} upgrade documentation and the
<a href="https://docs.aws.amazon.com/streams/latest/dev/kcl-migration-from-3-3-5.html" target="_blank" rel="noreferrer">AWS guide to migrating to the KCL single table format</a>.
</span>
</#if>
