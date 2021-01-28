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
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.Uninterruptibles;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.buffers.ProcessBuffer;
import org.graylog2.shared.messageq.AbstractMessageQueueReader;
import org.graylog2.shared.messageq.MessageQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.Message;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Singleton
public class SqsMessageQueueReader extends AbstractMessageQueueReader {

    private static final Logger LOG = LoggerFactory.getLogger(SqsMessageQueueReader.class);

    private final String queueUrl;

    private final Meter messageMeter;
    private final Counter byteCounter;
    private final Meter byteMeter;
    private final Timer readTimer;

    private final Provider<ProcessBuffer> processBufferProvider;
    private ProcessBuffer processBuffer;

    // TODO: check if having two distinct clients for receiving and deleting really gains us much
    private SqsAsyncClient receiveClient;
    private SqsAsyncClient deleteClient;

    private final Semaphore receiveSemaphore;
    private final Semaphore deleteSemaphore;

    @Inject
    public SqsMessageQueueReader(MetricRegistry metricRegistry, Provider<ProcessBuffer> processBufferProvider,
            EventBus eventBus, BaseConfiguration config) {
        super(eventBus);

        // Using a ProcessBuffer directly will lead to guice error:
        // "Please wait until after injection has completed to use this object."
        this.processBufferProvider = processBufferProvider;
        this.queueUrl = config.getSqsQueueUrl()
                .toString();

        receiveSemaphore = new Semaphore(config.getSqsMaxInflightReceiveBatches());
        deleteSemaphore = new Semaphore(config.getSqsMaxInflightOutboundBatches());

        this.messageMeter = metricRegistry.meter("system.message-queue.sqs.reader.messages");
        this.byteCounter = metricRegistry.counter("system.message-queue.sqs.reader.byte-count");
        this.byteMeter = metricRegistry.meter("system.message-queue.sqs.reader.bytes");
        this.readTimer = metricRegistry.timer("system.message-queue.sqs.reader.reads");
        metricRegistry.register("system.message-queue.sqs.reader.in-flight-receive-batches",
                (Gauge<Integer>) () -> config.getSqsMaxInflightReceiveBatches() - receiveSemaphore.availablePermits());
    }

    @Override
    protected void startUp() throws Exception {
        super.startUp();

        LOG.info("Starting SQS message queue reader service");

        final SqsAsyncClientBuilder clientBuilder = SqsAsyncClient.builder()
                .httpClientBuilder(NettyNioAsyncHttpClient.builder());
        this.receiveClient = clientBuilder.build();
        this.deleteClient = clientBuilder.build();

        processBuffer = processBufferProvider.get();
    }

    @Override
    protected void shutDown() throws Exception {
        if (receiveClient != null) {
            receiveClient.close();
        }
        if (deleteClient != null) {
            deleteClient.close();
        }

        super.shutDown();
    }

    @Override
    protected void run() throws Exception {

        while (isRunning()) {
            if (!shouldBeReading()) {
                Uninterruptibles.sleepUninterruptibly(100, MILLISECONDS);
                continue;
            }

            receiveNextBatchAsync();
        }
    }

    // TODO: slow down reading if the queues are empty
    private void receiveNextBatchAsync() throws InterruptedException {
        receiveSemaphore.acquire();
        if (!shouldBeReading()) {
            return;
        }

        final Timer.Context readTime = readTimer.time();
        receiveClient.receiveMessage(receiveRequest -> receiveRequest.queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(20))
                .whenComplete((response, error) -> {
                    readTime.stop();
                    if (error != null) {
                        LOG.error("Receiving messages from SQS failed", error);
                        // TODO maybe use exponential backoff
                        Uninterruptibles.sleepUninterruptibly(500, MILLISECONDS);
                    } else {
                        response.messages()
                                .forEach(this::handleMessage);
                    }
                    receiveSemaphore.release();
                });
    }

    private void handleMessage(Message message) {
        messageMeter.mark();
        byteCounter.inc(message.body()
                .length());
        byteMeter.mark(message.body()
                .length());
        try {
            final MessageQueue.Entry entry = SqsMessageQueueEntry.fromMessage(message);
            final RawMessage rawMessage = RawMessage.decode(entry.value(), entry.commitId());
            processBuffer.insertBlocking(rawMessage);
        } catch (Exception e) {
            LOG.error("Unable to convert message <" + message.messageId() + ">. Message will be deleted from " +
                    "message bus.", e);
            commit(message.receiptHandle());
        }
    }

    // TODO: this method shouldn't be used because sending batches with only a single item is really hurting
    //  performance. Unfortunately it's the main method used by the BenchmarkOutput so we probably need to buffer
    //  messages manually until we have enough of them to create a batch.
    public void commit(Object receiptHandle) {
        commit(Collections.singletonList(receiptHandle));
    }

    public void commit(List<Object> messageIds) {
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
        deleteClient.deleteMessageBatch(request -> request.queueUrl(queueUrl)
                .entries(entries))
                .whenComplete((response, error) -> {
                    if (error != null) {
                        LOG.error("Couldn't delete message", error);
                    }
                    deleteSemaphore.release();
                });
    }
}
