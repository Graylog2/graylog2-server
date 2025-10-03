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
package org.graylog.aws.inputs.cloudtrail.external;

import com.google.common.annotations.VisibleForTesting;
import jakarta.ws.rs.BadRequestException;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import javax.annotation.Nullable;
import java.net.URI;

/**
 * Factory for creating CloudTrail AWS clients.
 */
public class CloudTrailClientFactory {
    private static final Logger LOG = LoggerFactory.getLogger(CloudTrailClientFactory.class);

    /**
     * Check if the provided AWS credentials are valid by attempting to receive messages from the specified SQS queue.
     *
     * @param sqsQueueName        The URL of the SQS queue to check
     * @param credentialsProvider AWS credentials provider
     * @param awsRegion           AWS region
     * @return JSON string indicating whether the credentials are valid
     * @throws Exception if the credentials are invalid or an error occurs
     */
    public String checkCredentials(String sqsQueueName, AwsCredentialsProvider credentialsProvider, String awsRegion) throws Exception {
        try {
            try (SqsClient client = SqsClient.builder().
                    credentialsProvider(credentialsProvider)
                    .region(Region.of(awsRegion))
                    .build()) {
                ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                        .queueUrl(sqsQueueName)
                        .maxNumberOfMessages(5)
                        .build();
                client.receiveMessage(receiveMessageRequest);
            }
            return "{\"result\":\"valid\"}";
        } catch (Exception e) {
            LOG.error("AWS Cloudtrail credentials invalid {}", ExceptionUtils.getRootCauseMessage(e));
            throw new BadRequestException(ExceptionUtils.getRootCauseMessage(e));
        }
    }

    public CloudTrailS3Client getS3Client(String awsRegion, AwsCredentialsProvider credentialsProvider,
                                          InputFailureRecorder inputFailureRecorder) {
        return getS3Client(null, awsRegion, credentialsProvider, inputFailureRecorder);
    }

    @VisibleForTesting
    CloudTrailS3Client getS3Client(@Nullable URI endpoint, String awsRegion, AwsCredentialsProvider credentialsProvider,
                                   InputFailureRecorder inputFailureRecorder) {
        return new CloudTrailS3Client(endpoint, awsRegion, credentialsProvider, inputFailureRecorder);
    }
}
