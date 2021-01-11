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
package org.graylog2.shared.messageq.sqs;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.io.BaseEncoding;
import com.google.common.util.concurrent.AbstractIdleService;
import org.graylog2.shared.buffers.RawMessageEvent;
import org.graylog2.shared.messageq.MessageQueueException;
import org.graylog2.shared.messageq.MessageQueueWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Singleton
public class SqsMessageQueueWriter extends AbstractIdleService implements MessageQueueWriter {

    private static final Logger LOG = LoggerFactory.getLogger(SqsMessageQueueWriter.class);

    private final String queueUrl;

    private final CountDownLatch latch = new CountDownLatch(1);
    private final Meter messageMeter;
    private final Counter byteCounter;
    private final Meter byteMeter;
    private final Timer writeTimer;

    private SqsClient sqsClient;

    @Inject
    public SqsMessageQueueWriter(MetricRegistry metricRegistry, @Named("sqs_queue_url") URI queueUrl) {
        this.queueUrl = queueUrl.toString();

        this.messageMeter = metricRegistry.meter("system.message-queue.sqs.writer.messages");
        this.byteCounter = metricRegistry.counter("system.message-queue.sqs.writer.byte-count");
        this.byteMeter = metricRegistry.meter("system.message-queue.sqs.writer.bytes");
        this.writeTimer = metricRegistry.timer("system.message-queue.sqs.writer.writes");
    }

    @Override
    protected void startUp() throws Exception {
        LOG.info("Starting sqs message queue writer service");

        this.sqsClient = SqsClient.builder()
                .region(Region.EU_WEST_1) // TODO: don't hardcode a region
                .build();

        // Service is ready for writing
        latch.countDown();
    }

    @Override
    protected void shutDown() throws Exception {
        if (sqsClient != null) {
            sqsClient.close();
        }
    }

    @Override
    public void write(List<RawMessageEvent> entries) throws MessageQueueException {
        try {
            latch.await();
        } catch (InterruptedException e) {
            LOG.info("Got interrupted", e);
            Thread.currentThread()
                    .interrupt();
            return;
        }
        if (!isRunning()) {
            throw new MessageQueueException("Message queue service is not running");
        }

        // TODO create batches to max out request size
        //  consider moving to version 1 of the SDK to get this out-of-the-box with AmazonSQSBufferedAsyncClient.
        //  version 2 doesn't support this yet
        //  see https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-client-side-buffering-request-batching.html
        for (final RawMessageEvent entry : entries) {
            final String body = BaseEncoding.base64()
                    .omitPadding()
                    .encode(entry.getEncodedRawMessage());

            try (final Timer.Context ignored = writeTimer.time()) {
                sqsClient.sendMessage(SendMessageRequest.builder()
                        .queueUrl(this.queueUrl)
                        .messageBody(body)
                        .build());
                messageMeter.mark();
                byteCounter.inc(body.length());
                byteMeter.mark(body.length());
            }
        }
    }
}
