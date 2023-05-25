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

import com.amazonaws.regions.Region;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Uninterruptibles;
import okhttp3.HttpUrl;
import org.graylog.aws.auth.AWSAuthProvider;
import org.graylog.aws.inputs.cloudtrail.json.CloudTrailRecord;
import org.graylog.aws.inputs.cloudtrail.messages.TreeReader;
import org.graylog.aws.inputs.cloudtrail.notifications.CloudtrailSNSNotification;
import org.graylog.aws.inputs.cloudtrail.notifications.CloudtrailSQSClient;
import org.graylog.aws.s3.S3Reader;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.graylog2.shared.utilities.StringUtils.f;

public class CloudTrailSubscriber extends Thread {
    private static final Logger LOG = LoggerFactory.getLogger(CloudTrailSubscriber.class);

    public static final int SLEEP_INTERVAL_SECS = 5;

    private volatile boolean stopped = false;
    private volatile boolean paused = false;
    private volatile CountDownLatch pausedLatch = new CountDownLatch(0);

    private final MessageInput sourceInput;

    private final Region sqsRegion;
    private final Region s3Region;
    private final String queueName;
    private final AWSAuthProvider authProvider;
    private final HttpUrl proxyUrl;
    private final ObjectMapper objectMapper;
    private final InputFailureRecorder inputFailureRecorder;

    public CloudTrailSubscriber(Region sqsRegion, Region s3Region, String queueName, MessageInput sourceInput,
                                AWSAuthProvider authProvider, HttpUrl proxyUrl, ObjectMapper objectMapper,
                                InputFailureRecorder inputFailureRecorder) {
        this.sqsRegion = sqsRegion;
        this.s3Region = s3Region;
        this.queueName = queueName;
        this.authProvider = authProvider;
        this.sourceInput = sourceInput;
        this.proxyUrl = proxyUrl;
        this.objectMapper = objectMapper;
        this.inputFailureRecorder = inputFailureRecorder;
    }

    public void pause() {
        paused = true;
        pausedLatch = new CountDownLatch(1);
    }

    // A ridiculous name because "resume" is already defined in the super class...
    public void unpause() {
        paused = false;
        pausedLatch.countDown();
    }

    @Override
    public void run() {

        LOG.debug("Starting CloudTrailSubscriber");
        CloudtrailSQSClient subscriber = new CloudtrailSQSClient(
                sqsRegion,
                queueName,
                authProvider,
                proxyUrl,
                objectMapper);

        TreeReader reader = new TreeReader(objectMapper);
        S3Reader s3Reader = new S3Reader(s3Region, proxyUrl, authProvider);

        // This looks weird but it actually makes sense! Believe me.
        while (!stopped) {
            while (!stopped) {
                if (paused) {
                    LOG.debug("Processing paused");
                    Uninterruptibles.awaitUninterruptibly(pausedLatch);
                }
                if (stopped) {
                    break;
                }

                List<CloudtrailSNSNotification> notifications;
                try {
                    notifications = subscriber.getNotifications();
                } catch (Exception e) {
                    inputFailureRecorder.setFailing(getClass(), "Could not read messages from SQS. This is most likely a misconfiguration of the plugin. Going into sleep loop and retrying.", e);
                    break;
                }
                LOG.debug("Subscriber returned [{}] notifications.", notifications.size());

                /*
                 * Break out and wait a few seconds until next attempt to avoid hammering AWS with SQS
                 * read requests while still being able to read lots of queued notifications without
                 * the sleep() between each.
                 */
                if (notifications.size() == 0) {
                    LOG.debug("No more messages to read from SQS. Going into sleep loop.");
                    break;
                }

                LOG.debug("Proceeding to read message content from S3.");
                for (CloudtrailSNSNotification n : notifications) {
                    try {

                        LOG.debug("Checking for CloudTrail notifications in SQS.");
                        List<CloudTrailRecord> records = reader.read(
                                s3Reader.readCompressed(
                                        n.getS3Bucket(),
                                        n.getS3ObjectKey()));

                        LOG.debug("[{}] records read from S3.", records.size());

                        for (CloudTrailRecord record : records) {

                            LOG.debug("Processing message content.");

                            /*
                             * We are using process and not processFailFast here even though we are using a
                             * queue system (SQS) that could just deliver the message again when we are out of
                             * internal Graylog2 capacity.
                             *
                             * Reason is that every notification in SQS contains batches of CloudTrail messages
                             * that must be handled separately by Graylog2 (this loop) and we can only acknowledge
                             * the SQS notification that may include multiple CloudTrail messages. If one single
                             * internal message write fails, we would have to leave the whole notification on the
                             * queue and then possibly duplicate messages that did not fail later in subsequent
                             * write attempts.
                             *
                             * lol computers.
                             */

                            if (LOG.isTraceEnabled()) {
                                LOG.trace("Processing cloud trail record: {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(record));
                            }

                            sourceInput.processRawMessage(new RawMessage(objectMapper.writeValueAsBytes(record)));
                        }

                        // All messages written. Ack notification.
                        subscriber.deleteNotification(n);
                        inputFailureRecorder.setRunning();
                    } catch (Exception e) {
                        inputFailureRecorder.setFailing(this.getClass(), f("Could not read CloudTrail log file for <%s>. Skipping.", n.getS3Bucket()), e);
                    }
                }
            }

            if (!stopped) {
                LOG.debug("Waiting {} seconds until next CloudTrail SQS check.", SLEEP_INTERVAL_SECS);
                Uninterruptibles.sleepUninterruptibly(SLEEP_INTERVAL_SECS, TimeUnit.SECONDS);
            }
        }
    }

    public void terminate() {
        stopped = true;
        paused = false;
        pausedLatch.countDown();
    }
}