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

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.buffered.QueueBufferConfig;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.DeleteMessageResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.QueueAttributeName;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.Uninterruptibles;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.buffers.ProcessBuffer;
import org.graylog2.shared.messageq.AbstractMessageQueueReader;
import org.graylog2.shared.messageq.MessageQueue;
import org.graylog2.shared.messageq.MessageQueueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Singleton
public class SqsMessageQueueReader extends AbstractMessageQueueReader {
    private static final Logger LOG = LoggerFactory.getLogger(SqsMessageQueueReader.class);

    private final BaseConfiguration config;
    private final String queueUrl;

    private final CountDownLatch latch = new CountDownLatch(1);
    private final Meter messageMeter;
    private final Counter byteCounter;
    private final Meter byteMeter;
    private final Timer readTimer;

    private final Provider<ProcessBuffer> processBufferProvider;
    private ProcessBuffer processBuffer;
    private AmazonSQSBufferedAsyncClient sqsClient;

    @Inject
    public SqsMessageQueueReader(MetricRegistry metricRegistry, Provider<ProcessBuffer> processBufferProvider,
            EventBus eventBus, BaseConfiguration config) {
        super(eventBus);

        // Using a ProcessBuffer directly will lead to guice error:
        // "Please wait until after injection has completed to use this object."
        this.processBufferProvider = processBufferProvider;
        this.config = config;
        this.queueUrl = config.getSqsQueueUrl().toString();

        this.messageMeter = metricRegistry.meter("system.message-queue.sqs.reader.messages");
        this.byteCounter = metricRegistry.counter("system.message-queue.sqs.reader.byte-count");
        this.byteMeter = metricRegistry.meter("system.message-queue.sqs.reader.bytes");
        this.readTimer = metricRegistry.timer("system.message-queue.sqs.reader.reads");
    }

    @Override
    protected void startUp() throws Exception {
        super.startUp();

        LOG.info("Starting pulsar message queue reader service");

        final QueueBufferConfig bufferConfig = new QueueBufferConfig()
                .withMaxInflightReceiveBatches(config.getSqsMaxInflightReceiveBatches())
                .withMaxInflightOutboundBatches(config.getSqsMaxInflightOutboundBatches())
                .withMaxDoneReceiveBatches(config.getSqsMaxDoneReceiveBatches());

        this.sqsClient = new AmazonSQSBufferedAsyncClient(AmazonSQSAsyncClientBuilder.defaultClient(), bufferConfig);

        processBuffer = processBufferProvider.get();

        // Service is ready for consuming
        latch.countDown();
    }

    @Override
    protected void shutDown() throws Exception {
        if (sqsClient != null) {
            sqsClient.shutdown();
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
            final List<MessageQueue.Entry> entries = read();
            entries.forEach(entry -> {
                LOG.debug("Consumed message: {}", entry);
                final RawMessage rawMessage = RawMessage.decode(entry.value(), entry.commitId());
                processBuffer.insertBlocking(rawMessage);
            });
        }
    }

    private List<MessageQueue.Entry> read() throws MessageQueueException {
        if (!isRunning()) {
            throw new MessageQueueException("Message queue service is not running");
        }

        final List<Message> messages = receiveEncodedMessages();
        return decodeMessages(messages);
    }

    private ImmutableList<MessageQueue.Entry> decodeMessages(List<Message> messages) {
        final ImmutableList.Builder<MessageQueue.Entry> builder = ImmutableList.builder();

        messages.forEach(message -> {
            try {
                final MessageQueue.Entry entry = SqsMessageQueueEntry.fromMessage(message);
                builder.add(entry);
                messageMeter.mark();
                byteCounter.inc(message.getBody().length());
                byteMeter.mark(message.getBody().length());
            } catch (Exception e) {
                LOG.error("Unable to convert message <" + message.getMessageId() + ">. Message will be deleted from " +
                        "message bus.", e);
                commit(message.getReceiptHandle());
            }
        });

        return builder.build();
    }

    private List<Message> receiveEncodedMessages() {
        try {
            final ReceiveMessageResult result = sqsClient.receiveMessage(
                    new ReceiveMessageRequest(queueUrl).withAttributeNames(QueueAttributeName.All)
                            .withMaxNumberOfMessages(10)
                            .withWaitTimeSeconds(20));
            return result.getMessages();
        } catch (Exception e) {
            LOG.error("Error consuming messages.", e);
        }
        return Collections.emptyList();
    }

    public void commit(Object receiptHandle) {
        if (!(receiptHandle instanceof String)) {
            LOG.error("Couldn't delete message. Expected <" + receiptHandle + "> to be a String receipt handle");
            return;
        }

        try {
            sqsClient.deleteMessageAsync(new DeleteMessageRequest(queueUrl, (String) receiptHandle),
                    new AsyncHandler<DeleteMessageRequest, DeleteMessageResult>() {
                @Override
                public void onError(Exception exception) {
                    LOG.error("Couldn't delete message", exception);
                }

                @Override
                public void onSuccess(DeleteMessageRequest request, DeleteMessageResult deleteMessageResult) {
                    // OK
                }
            });
        } catch (Exception e) {
            LOG.error("Couldn't send delete request to SQS.", e);
        }
    }
}
