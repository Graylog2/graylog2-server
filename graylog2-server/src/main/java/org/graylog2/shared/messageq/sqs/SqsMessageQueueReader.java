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
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.Uninterruptibles;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.buffers.ProcessBuffer;
import org.graylog2.shared.messageq.AbstractMessageQueueReader;
import org.graylog2.shared.messageq.MessageQueue;
import org.graylog2.shared.messageq.MessageQueueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Singleton
public class SqsMessageQueueReader extends AbstractMessageQueueReader {
    private static final Logger LOG = LoggerFactory.getLogger(SqsMessageQueueReader.class);

    private final String queueUrl;

    private final CountDownLatch latch = new CountDownLatch(1);
    private final Meter messageMeter;
    private final Counter byteCounter;
    private final Meter byteMeter;
    private final Timer readTimer;

    private final Provider<ProcessBuffer> processBufferProvider;
    private ProcessBuffer processBuffer;
    private SqsClient sqsClient;

    @Inject
    public SqsMessageQueueReader(MetricRegistry metricRegistry, Provider<ProcessBuffer> processBufferProvider,
            EventBus eventBus, @Named("sqs_queue_url") URI queueUrl) {
        super(eventBus);

        // Using a ProcessBuffer directly will lead to guice error:
        // "Please wait until after injection has completed to use this object."
        this.processBufferProvider = processBufferProvider;
        this.queueUrl = queueUrl.toString();

        this.messageMeter = metricRegistry.meter("system.message-queue.sqs.reader.messages");
        this.byteCounter = metricRegistry.counter("system.message-queue.sqs.reader.byte-count");
        this.byteMeter = metricRegistry.meter("system.message-queue.sqs.reader.bytes");
        this.readTimer = metricRegistry.timer("system.message-queue.sqs.reader.reads");
    }

    @Override
    protected void startUp() throws Exception {
        super.startUp();

        LOG.info("Starting pulsar message queue reader service");

        this.sqsClient = SqsClient.builder()
                .region(Region.EU_WEST_1) // TODO: don't hardcode a region
                .build();

        processBuffer = processBufferProvider.get();

        // Service is ready for consuming
        latch.countDown();
    }

    @Override
    protected void shutDown() throws Exception {
        if (sqsClient != null) {
            sqsClient.close();
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
        final ImmutableList.Builder<MessageQueue.Entry> builder = ImmutableList.builder();

        if (!isRunning()) {
            throw new MessageQueueException("Message queue service is not running");
        }

        List<Message> messages = Collections.emptyList();
        try {
            // doing this in a single thread offers poor performance. according to the documentation we would only
            // get about 50 requests per second this way: https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/sqs-throughput-horizontal-scaling-and-batching.html
            final ReceiveMessageResponse response = sqsClient.receiveMessage(
                    ReceiveMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .attributeNames(QueueAttributeName.ALL)
                            .waitTimeSeconds(20)
                            .maxNumberOfMessages(10)
                            .build());
            messages = response.messages();
        } catch (Exception e) {
            LOG.error("Error consuming messages.", e);
        }

        messages.forEach(message -> {
            try {
                final MessageQueue.Entry entry = SqsMessageQueueEntry.fromMessage(message);
                builder.add(entry);
                messageMeter.mark();
                byteCounter.inc(message.body().length());
                byteMeter.mark(message.body().length());
            } catch (Exception e) {
                LOG.error("Unable to convert message <" + message.messageId() + ">. Message will be deleted from " +
                        "message bus.", e);
                commit(message.receiptHandle());
            }
        });

        return builder.build();
    }

    // TODO: support batch commits
    public void commit(Object receiptHandle) {
        if (receiptHandle instanceof String) {
            try {
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle((String) receiptHandle)
                        .build());
            } catch (Exception e) {
                LOG.error("Couldn't delete message", e);
            }
        } else {
            LOG.error("Couldn't delete message. Expected <" + receiptHandle + "> to be a String receipt handle");
        }
    }
}
