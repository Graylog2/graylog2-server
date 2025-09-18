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
package org.graylog.aws.inputs.cloudtrail;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.aws.inputs.cloudtrail.external.CloudTrailS3Client;
import org.graylog.aws.inputs.cloudtrail.json.CloudTrailRecord;
import org.graylog.aws.inputs.cloudtrail.messages.TreeReader;
import org.graylog.aws.inputs.cloudtrail.sqs.CloudTrailSQSReader;
import org.graylog.aws.notifications.SQSClient;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.graylog2.shared.utilities.StringUtils.f;

public class CloudTrailPollerTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(CloudTrailPollerTask.class);
    private static final int BATCH_SIZE = 10;
    private final MessageInput input;
    private final CloudTrailTransport transport;
    private final CloudTrailS3Client cloudTrailS3Client;
    private final InputFailureRecorder failureRecorder;
    private final CloudTrailSQSReader sqsReader;
    private final ObjectMapper objectMapper;

    public CloudTrailPollerTask(MessageInput input,
                                SQSClient sqsClient,
                                CloudTrailS3Client cloudTrailS3Client,
                                CloudTrailTransport transport,
                                InputFailureRecorder failureRecorder,
                                ObjectMapper objectMapper,
                                AtomicBoolean interrupt) {
        this.input = input;
        this.transport = transport;
        this.failureRecorder = failureRecorder;
        this.objectMapper = objectMapper;
        this.cloudTrailS3Client = cloudTrailS3Client;
        this.sqsReader = new CloudTrailSQSReader(interrupt, sqsClient, failureRecorder, BATCH_SIZE);
    }

    @Override
    public void run() {
        try {
            doRun();
        } catch (Throwable t) {
            failureRecorder.setFailing(getClass(), "Failure in CloudTrail Poller Task", t);
        }
    }

    public void doRun() throws IOException {
        if (transport.isThrottled()) {
            LOG.debug("[throttled] The CloudTrail input will pause message processing until the throttle state clears.");
            transport.blockUntilUnthrottled();
            LOG.debug("[unthrottled] The CloudTrail input will now resume processing records.");
        }

        final AtomicInteger totalProcessedRecords = new AtomicInteger();
        sqsReader.read(notification -> {
            // Callback for reading S3 payload from SQS notification.
            try {
                LOG.debug("Checking for CloudTrail notifications in SQS.");
                TreeReader treeReader = new TreeReader(objectMapper);
                List<CloudTrailRecord> records = treeReader.read(
                        cloudTrailS3Client.readCompressed(
                                notification.getS3Bucket(),
                                notification.getS3ObjectKey()));

                LOG.debug("[{}] records read from S3.", records.size());

                for (CloudTrailRecord record : records) {
                    LOG.debug("Processing CloudTrail message content.");

                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Processing cloud trail record: {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(record));
                    }

                    input.processRawMessage(new RawMessage(objectMapper.writeValueAsBytes(record)));
                    totalProcessedRecords.incrementAndGet();
                }
            } catch (Exception e) {
                final String msg = f(
                        "Could not read CloudTrail log file for <%s>. Skipping.", notification.getS3Bucket(),
                        ExceptionUtils.getRootCauseMessage(e));
                failureRecorder.setFailing(getClass(), msg, e);
            }
        });

        if (totalProcessedRecords.get() > 0) {
            failureRecorder.setRunning();
        }
        LOG.debug("Total records processed for Input [{}] : [{}]", input.toIdentifier(), totalProcessedRecords);
    }
}
