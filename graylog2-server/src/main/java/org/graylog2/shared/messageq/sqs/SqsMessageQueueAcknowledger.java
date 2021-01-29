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


import com.google.common.util.concurrent.AbstractIdleService;
import de.huxhorn.sulky.ulid.ULID;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.shared.messageq.MessageQueueAcknowledger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

@Singleton
public class SqsMessageQueueAcknowledger extends AbstractIdleService implements MessageQueueAcknowledger {

    private static final Logger LOG = LoggerFactory.getLogger(SqsMessageQueueAcknowledger.class);

    private final CountDownLatch readyLatch = new CountDownLatch(1);
    private final ULID ulid = new ULID();
    private final String queueUrl;
    private final Semaphore deleteSemaphore;
    private final BatchAggregator<DeleteMessageBatchRequestEntry> batchAggregator;

    private SqsAsyncClient sqsClient;

    @Inject
    public SqsMessageQueueAcknowledger(BaseConfiguration config) {
        this.queueUrl = config.getSqsQueueUrl().toString();
        this.deleteSemaphore = new Semaphore(config.getSqsMaxInflightOutboundBatches());
        this.batchAggregator = new BatchAggregator<>(this::sendBatch, 10, Duration.ofSeconds(1));
    }

    @Override
    protected void startUp() throws Exception {
        LOG.info("Starting SQS message queue deletion service");

        final SqsAsyncClientBuilder clientBuilder = SqsAsyncClient.builder()
                .httpClientBuilder(NettyNioAsyncHttpClient.builder());
        this.sqsClient = clientBuilder.build();

        batchAggregator.start();

        // Service is ready for writing
       readyLatch.countDown();
    }

    @Override
    protected void shutDown() throws Exception {
        // make sure that we can flush all remaining batches out
        batchAggregator.shutdown();
        if (sqsClient != null) {
            sqsClient.close();
        }
    }

    @Override
    public void acknowledge(Object receiptHandle) {
        acknowledge(Collections.singletonList(receiptHandle));
    }

    @Override
    public void acknowledge(List<Object> receiptHandles) {
        try {
            readyLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        receiptHandles.stream()
                .filter(handle -> {
                    if (!(handle instanceof String)) {
                        LOG.error("Couldn't delete message. Expected <{}> to be a String receipt handle", handle);
                        return false;
                    }
                    return true;
                })
                .map(String.class::cast)
                .map(handle -> DeleteMessageBatchRequestEntry.builder()
                        .receiptHandle(handle)
                        .id(ulid.nextULID())
                        .build())
                .forEach(batchAggregator::feed);
    }

    private void sendBatch(List<DeleteMessageBatchRequestEntry> entries) {
        try {
            readyLatch.await();
            deleteSemaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        sqsClient.deleteMessageBatch(request -> request.queueUrl(queueUrl)
                .entries(entries))
                .whenComplete((response, error) -> {
                    if (error != null) {
                        LOG.error("Couldn't delete message", error);
                    }
                    deleteSemaphore.release();
                });
    }
}
