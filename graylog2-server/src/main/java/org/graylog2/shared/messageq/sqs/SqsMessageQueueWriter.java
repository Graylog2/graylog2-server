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
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.google.common.util.concurrent.AbstractIdleService;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.shared.buffers.RawMessageEvent;
import org.graylog2.shared.messageq.MessageQueueException;
import org.graylog2.shared.messageq.MessageQueueWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

@Singleton
public class SqsMessageQueueWriter extends AbstractIdleService implements MessageQueueWriter {
    private static final Logger LOG = LoggerFactory.getLogger(SqsMessageQueueWriter.class);

    private final String queueUrl;

    private final CountDownLatch latch = new CountDownLatch(1);
    private final Meter messageMeter;
    private final Counter byteCounter;
    private final Meter byteMeter;
    private final Timer writeTimer;

    private SqsAsyncClient sqsClient;

    private final Semaphore inFlightBatchesSemaphore;

    @Inject
    public SqsMessageQueueWriter(MetricRegistry metricRegistry, @Named("sqs_queue_url") URI queueUrl,
            BaseConfiguration config) {
        this.queueUrl = queueUrl.toString();

        inFlightBatchesSemaphore = new Semaphore(config.getSqsMaxInflightOutboundBatches());

        this.messageMeter = metricRegistry.meter("system.message-queue.sqs.writer.messages");
        this.byteCounter = metricRegistry.counter("system.message-queue.sqs.writer.byte-count");
        this.byteMeter = metricRegistry.meter("system.message-queue.sqs.writer.bytes");
        this.writeTimer = metricRegistry.timer("system.message-queue.sqs.writer.writes");
        metricRegistry.register("system.message-queue.sqs.writer.in-flight-outbound-batches",
                (Gauge<Integer>) () -> config.getSqsMaxInflightOutboundBatches() -
                        inFlightBatchesSemaphore.availablePermits());
    }

    @Override
    protected void startUp() throws Exception {
        LOG.info("Starting sqs message queue writer service");

        this.sqsClient = SqsAsyncClient.builder()
                .httpClientBuilder(NettyNioAsyncHttpClient.builder()).build();

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
        // TODO: decide what we want to do in case of an error. We really don't want to simply discard messages just
        //  because SQS is down.
        try {
            latch.await();
            if (!isRunning()) {
                throw new MessageQueueException("Message queue service is not running");
            }
            // TODO: is it worth creating batches manually (performance-wise)?
            for (List<RawMessageEvent> batch : Lists.partition(entries, 10)) {
                sendBatchAsync(batch);
            }
        } catch (InterruptedException e) {
            LOG.info("Got interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    private void sendBatchAsync(List<RawMessageEvent> rawMessageEvents) throws InterruptedException {
        int batchSizeBytes = 0;
        List<SendMessageBatchRequestEntry> batchRequestEntries = new ArrayList<>();

        for (int i = 0; i < rawMessageEvents.size(); i++) {
            final RawMessageEvent rawMessageEvent = rawMessageEvents.get(i);
            final String encodedMessage = BaseEncoding.base64()
                    .omitPadding()
                    .encode(rawMessageEvent.getEncodedRawMessage());
            batchSizeBytes += encodedMessage.length();
            batchRequestEntries.add(SendMessageBatchRequestEntry.builder()
                    .messageBody(encodedMessage)
                    .id(String.valueOf(i))
                    .build());
        }

        inFlightBatchesSemaphore.acquire();
        final Timer.Context writeTime = writeTimer.time();

        sqsClient.sendMessageBatch(batchRequest -> batchRequest.entries(batchRequestEntries)
                .queueUrl(queueUrl))
                .whenComplete((response, error) -> {
                    writeTime.stop();
                    if (error != null) {
                        LOG.error("Sending message batch failed", error);
                    }
                    inFlightBatchesSemaphore.release();
                });
        messageMeter.mark(batchRequestEntries.size());
        byteCounter.inc(batchSizeBytes);
        byteMeter.mark(batchSizeBytes);
    }
}
