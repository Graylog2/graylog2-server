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
package org.graylog.aws.inputs.cloudtrail.api.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.security.encryption.EncryptedValue;

import javax.annotation.Nullable;

/**
 * This interface holds user input request for CloudTrail.
 */
@JsonAutoDetect
public interface CloudTrailRequest {
    String AWS_ACCESS_ID = "aws_access_key";
    String AWS_SECRET_KEY = "aws_secret_key";
    String AWS_CLOUDTRAIL_QUEUE_NAME = "cloudtrail_queue_name";
    String AWS_REGION = "aws_region";
    String ASSUME_ROLE_ARN = "assume_role_arn";

    @Nullable
    @JsonProperty(AWS_ACCESS_ID)
    String accessKeyId();

    @Nullable
    @JsonProperty(AWS_SECRET_KEY)
    EncryptedValue secretAccessKey();

    @Nullable
    @JsonProperty(AWS_CLOUDTRAIL_QUEUE_NAME)
    String sqsQueueName();

    @Nullable
    @JsonProperty(AWS_REGION)
    String sqsRegion();

    @Nullable
    @JsonProperty(ASSUME_ROLE_ARN)
    String assumeRoleArn();

    interface Builder<SELF> {

        @JsonProperty(AWS_ACCESS_ID)
        SELF accessKeyId(String accessId);

        @JsonProperty(AWS_SECRET_KEY)
        SELF secretAccessKey(EncryptedValue secretAccessKey);

        @JsonProperty(AWS_CLOUDTRAIL_QUEUE_NAME)
        SELF sqsQueueName(String sqsQueueName);

        @JsonProperty(AWS_REGION)
        SELF sqsRegion(String sqsRegion);

        @JsonProperty(ASSUME_ROLE_ARN)
        SELF assumeRoleArn(String assumeRoleArn);
    }
}
