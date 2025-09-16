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
package org.graylog.aws.inputs.cloudtrail.sqs;

import org.graylog.aws.notifications.SNSNotification;
import org.graylog.aws.notifications.SQSClient;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Polls SQS notifications, parses them, and pases the notification to the provided S3 callback.
 * Each caller supplies their own S3 reader, since files can be in caller-specific formats.
 */
public class CloudTrailSQSReader {
    private static final Logger LOG = LoggerFactory.getLogger(CloudTrailSQSReader.class);
    private final AtomicBoolean interrupt;
    private final SQSClient sqsClient;
    private final InputFailureRecorder failureRecorder;
    private final int sqsMessageBatchSize;

    public CloudTrailSQSReader(AtomicBoolean interrupt, SQSClient sqsClient, InputFailureRecorder failureRecorder, int sqsMessageBatchSize) {
        this.interrupt = interrupt;
        this.sqsClient = sqsClient;
        this.failureRecorder = failureRecorder;
        this.sqsMessageBatchSize = sqsMessageBatchSize;
    }

    public void read(Consumer<SNSNotification> s3Callback) {
        List<SNSNotification> sqsNotifications;
        do {
            // Stop processing messages if the input is stopped. The messages will stay in the SQS queue and can be read later.
            if (interrupt.get()) {
                LOG.debug("Shutdown request received.");
                return;
            }
            try {
                sqsNotifications = sqsClient.getNotifications(sqsMessageBatchSize);
            } catch (Throwable e) {
                final String msg = f(
                        "Error fetching manifest for Logs: [%s]", e.getMessage(),
                        ExceptionUtils.getRootCauseMessage(e));
                failureRecorder.setFailing(getClass(), msg, e);
                return;
            }

            LOG.debug("Received [{}] notifications from SQS.", sqsNotifications.size());
            for (SNSNotification notification : sqsNotifications) {
                s3Callback.accept(notification);
                sqsClient.deleteNotification(notification);
            }
        } while (!sqsNotifications.isEmpty());
    }
}
