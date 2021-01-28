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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

@Singleton
public class SqsMessageQueueAcknowledger extends AbstractIdleService implements MessageQueueAcknowledger {

    private static final Logger LOG = LoggerFactory.getLogger(SqsMessageQueueAcknowledger.class);
    private final CountDownLatch readyLatch = new CountDownLatch(1);

    private final String queueUrl;
    private final Semaphore deleteSemaphore;

    private SqsAsyncClient sqsClient;

    @Inject
    public SqsMessageQueueAcknowledger(BaseConfiguration config) {
        this.queueUrl = config.getSqsQueueUrl().toString();
        deleteSemaphore = new Semaphore(config.getSqsMaxInflightOutboundBatches());
    }

    @Override
    protected void startUp() throws Exception {
        LOG.info("Starting SQS message queue deletion service");

        final SqsAsyncClientBuilder clientBuilder = SqsAsyncClient.builder()
                .httpClientBuilder(NettyNioAsyncHttpClient.builder());
        this.sqsClient = clientBuilder.build();

        // Service is ready for writing
        readyLatch.countDown();
    }

    @Override
    protected void shutDown() throws Exception {
        if (sqsClient != null) {
            sqsClient.close();
        }
    }

    // TODO: this method shouldn't be used because sending batches with only a single item is really hurting
    //  performance. Unfortunately it's the main method used by the BenchmarkOutput so we probably need to buffer
    //  messages manually until we have enough of them to create a batch.
    @Override
    public void acknowledge(Object receiptHandle) {
        acknowledge(Collections.singletonList(receiptHandle));
    }

    @Override
    public void acknowledge(List<Object> messageIds) {
        try {
            readyLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        final Iterator<Object> iterator = messageIds.iterator();

        List<DeleteMessageBatchRequestEntry> currentBatch = new ArrayList<>(10);

        while (iterator.hasNext()) {
            Object receiptHandle = iterator.next();
            if (!(receiptHandle instanceof String)) {
                LOG.error("Couldn't delete message. Expected <" + receiptHandle + "> to be a String receipt handle");
                continue;
            }
            final DeleteMessageBatchRequestEntry entry = DeleteMessageBatchRequestEntry.builder()
                    .receiptHandle((String) receiptHandle)
                    .id(String.valueOf(currentBatch.size() + 1))
                    .build();
            currentBatch.add(entry);

            if (currentBatch.size() == 10) {
                deleteBatch(currentBatch);
                currentBatch = new ArrayList<>(10);
            }
        }
        if (!currentBatch.isEmpty()) {
            deleteBatch(currentBatch);
        }
    }

    private void deleteBatch(List<DeleteMessageBatchRequestEntry> entries) {
        try {
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
