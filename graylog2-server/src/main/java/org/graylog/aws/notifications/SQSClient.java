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
package org.graylog.aws.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.graylog.aws.sqs.ObjectCreatedPutParseException;
import org.graylog2.plugin.InputFailureRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.List;

public class SQSClient {
    private static final Logger LOG = LoggerFactory.getLogger(SQSClient.class);

    private final SqsClient sqs;
    private final String queueName;
    private final ObjectMapper objectMapper;
    private final InputFailureRecorder inputFailureRecorder;

    public SQSClient(String queueName, String region, AwsCredentialsProvider authProvider, ObjectMapper objectMapper,
                     InputFailureRecorder inputFailureRecorder) {
        SqsClientBuilder clientBuilder = SqsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(authProvider);
        this.sqs = clientBuilder.build();
        this.queueName = queueName;
        this.objectMapper = objectMapper;
        this.inputFailureRecorder = inputFailureRecorder;
    }

    public List<SNSNotification> getNotifications(int sqsMessageBatchSize) {
        LOG.debug("Attempting to read SQS notifications with batch size of [{}].", sqsMessageBatchSize);
        List<SNSNotification> notifications = Lists.newArrayList();
        ReceiveMessageRequest receiveMessageRequest;
        receiveMessageRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueName)
                .maxNumberOfMessages(sqsMessageBatchSize)
                .visibilityTimeout(30)
                // Small wait for more messages to arrive.
                .waitTimeSeconds(5)
                .build();
        final ReceiveMessageResponse result = sqs.receiveMessage(receiveMessageRequest);
        LOG.debug("Received [{}] SQS notifications.", result.messages().size());
        SNSNotificationParser parser = new SNSNotificationParser(objectMapper);
        for (Message message : result.messages()) {
            final List<SNSNotification> messages;
            try {
                messages = parser.parse(message);
            } catch (ObjectCreatedPutParseException e) {
                inputFailureRecorder.setFailing(getClass(), e.getMessage());
                deleteNotification(new SNSNotification(e.getReceiptHandle(), null, null));
                continue;
            }
            notifications.addAll(messages);
        }
        inputFailureRecorder.setRunning();
        return notifications;
    }

    public void deleteNotification(SNSNotification notification) {
        try {
            sqs.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(queueName)
                    .receiptHandle(notification.getReceiptHandle())
                    .build());
            inputFailureRecorder.setRunning();
            LOG.debug("Deleted SQS notification <{}>.", notification.getReceiptHandle());
        } catch (Exception e) {
            throw new RuntimeException("Error in deleting the notification", e);
        }
    }
}
