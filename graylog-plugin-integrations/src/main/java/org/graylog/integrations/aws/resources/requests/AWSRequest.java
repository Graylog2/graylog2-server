package org.graylog.integrations.aws.resources.requests;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;

/**
 * All AWS API requests should implement this interface.
 * These three fields are needed for all requests.
 */

public interface AWSRequest {

    // Constants are defined here once for all classes.
    String REGION = "region";
    String AWS_ACCESS_KEY_ID = "aws_access_key_id";
    String AWS_SECRET_ACCESS_KEY = "aws_secret_access_key";
    String ASSUME_ROLE_ARN = "assume_role_arn";

    String CLOUDWATCH_ENDPOINT = "cloudwatch_endpoint";
    String DYNAMODB_ENDPOINT = "dynamodb_endpoint";
    String IAM_ENDPOINT = "iam_endpoint";
    String KINESIS_ENDPOINT = "kinesis_endpoint";

    @JsonProperty(REGION)
    String region();

    @Nullable
    @JsonProperty(AWS_ACCESS_KEY_ID)
    String awsAccessKeyId();

    @Nullable
    @JsonProperty(AWS_SECRET_ACCESS_KEY)
    String awsSecretAccessKey();

    @Nullable
    @JsonProperty(ASSUME_ROLE_ARN)
    String assumeRoleArn();

    @Nullable
    @JsonProperty(CLOUDWATCH_ENDPOINT)
    String cloudwatchEndpoint();

    @Nullable
    @JsonProperty(DYNAMODB_ENDPOINT)
    String dynamodbEndpoint();

    @Nullable
    @JsonProperty(IAM_ENDPOINT)
    String iamEndpoint();

    @Nullable
    @JsonProperty(KINESIS_ENDPOINT)
    String kinesisEndpoint();

    interface Builder<SELF> {
        @JsonProperty(REGION)
        SELF region(String region);

        @JsonProperty(AWS_ACCESS_KEY_ID)
        SELF awsAccessKeyId(String awsAccessKeyId);

        @JsonProperty(AWS_SECRET_ACCESS_KEY)
        SELF awsSecretAccessKey(String awsSecretAccessKey);

        @JsonProperty(ASSUME_ROLE_ARN)
        SELF assumeRoleArn(String assumeRoleArn);

        @JsonProperty(CLOUDWATCH_ENDPOINT)
        SELF cloudwatchEndpoint(String cloudwatchEndpoint);

        @JsonProperty(DYNAMODB_ENDPOINT)
        SELF dynamodbEndpoint(String dynamodbEndpoint);

        @JsonProperty(IAM_ENDPOINT)
        SELF iamEndpoint(String iamEndpoint);

        @JsonProperty(KINESIS_ENDPOINT)
        SELF kinesisEndpoint(String kinesisEndpoint);
    }
}