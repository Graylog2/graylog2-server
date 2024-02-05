/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.integrations.aws.resources.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.security.encryption.EncryptedValue;

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
    EncryptedValue awsSecretAccessKey();

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
        SELF awsSecretAccessKey(EncryptedValue awsSecretAccessKey);

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
