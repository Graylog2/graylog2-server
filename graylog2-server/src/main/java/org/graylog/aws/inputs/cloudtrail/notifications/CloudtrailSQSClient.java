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
package org.graylog.aws.inputs.cloudtrail.notifications;

import com.amazonaws.regions.Region;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import okhttp3.HttpUrl;
import org.graylog.aws.auth.AWSAuthProvider;
import org.graylog.aws.config.Proxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CloudtrailSQSClient {
    private static final Logger LOG = LoggerFactory.getLogger(CloudtrailSQSClient.class);

    private final AmazonSQS sqs;
    private final String queueName;
    private final ObjectMapper objectMapper;

    public CloudtrailSQSClient(Region region, String queueName, AWSAuthProvider authProvider, HttpUrl proxyUrl, ObjectMapper objectMapper) {
        AmazonSQSClientBuilder clientBuilder = AmazonSQSClientBuilder.standard().withRegion(region.getName()).withCredentials(authProvider);

        if (proxyUrl != null) {
            clientBuilder.withClientConfiguration(Proxy.forAWS(proxyUrl));
        }

        this.sqs = clientBuilder.build();

        this.queueName = queueName;
        this.objectMapper = objectMapper;
    }

    public List<CloudtrailSNSNotification> getNotifications() {

        LOG.debug("Fetching SQS CloudTrail notifications.");

        List<CloudtrailSNSNotification> notifications = Lists.newArrayList();

        ReceiveMessageRequest request = new ReceiveMessageRequest(queueName);
        request.setMaxNumberOfMessages(10);
        ReceiveMessageResult result = sqs.receiveMessage(request);

        LOG.debug("Received [{}] SQS CloudTrail notifications.", result.getMessages().size());
        CloudtrailSNSNotificationParser parser = new CloudtrailSNSNotificationParser(objectMapper);
        LOG.debug("Finished parsing notifications.");

        for (Message message : result.getMessages()) {
            notifications.addAll(parser.parse(message));
        }

        LOG.debug("Returning notifications.");

        return notifications;
    }

    public void deleteNotification(CloudtrailSNSNotification notification) {
        LOG.debug("Deleting SQS CloudTrail notification <{}>.", notification.getReceiptHandle());

        sqs.deleteMessage(new DeleteMessageRequest()
                .withQueueUrl(queueName)
                .withReceiptHandle(notification.getReceiptHandle()));
    }
}