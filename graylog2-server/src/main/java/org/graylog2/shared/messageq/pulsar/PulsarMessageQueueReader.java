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
package org.graylog2.shared.messageq.pulsar;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import org.apache.pulsar.client.api.BatchReceivePolicy;
import org.apache.pulsar.client.api.ConsumerInterceptor;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Messages;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.buffers.ProcessBuffer;
import org.graylog2.shared.messageq.MessageQueue;
import org.graylog2.shared.messageq.MessageQueueException;
import org.graylog2.shared.messageq.MessageQueueReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static com.codahale.metrics.MetricRegistry.name;

@Singleton
public class PulsarMessageQueueReader extends AbstractExecutionThreadService implements MessageQueueReader {

    private static final Logger LOG = LoggerFactory.getLogger(PulsarMessageQueueReader.class);

    private final String name;
    private final String topic;
    private final String serviceUrl;

    private final CountDownLatch latch = new CountDownLatch(1);
    private final Meter messageMeter;
    private final Counter byteCounter;
    private final Meter byteMeter;
    private final Timer readTimer;

    private PulsarClient client;
    private org.apache.pulsar.client.api.Consumer<byte[]> consumer;
    private Provider<ProcessBuffer> processBufferProvider;
    private ProcessBuffer processBuffer;

    @Inject
    public PulsarMessageQueueReader(MetricRegistry metricRegistry,
                                    Provider<ProcessBuffer> processBufferProvider) {
        // Using a ProcessBuffer directly will lead to guice error:
        // "Please wait until after injection has completed to use this object."
        this.processBufferProvider = processBufferProvider;
        this.name = "input"; // TODO: use cluster-id?
        this.topic = name + "-message-queue"; // TODO: Make configurable
        this.serviceUrl = "pulsar://localhost:6650"; // TODO: Make configurable

        this.messageMeter = metricRegistry.meter(name("system.message-queue.pulsar", name, "reader.messages"));
        this.byteCounter = metricRegistry.counter(name("system.message-queue.pulsar", name, "reader.byte-count"));
        this.byteMeter = metricRegistry.meter(name("system.message-queue.pulsar", name, "reader.bytes"));
        this.readTimer = metricRegistry.timer(name("system.message-queue.pulsar", name, "reader.reads"));
    }

    @Override
    protected void startUp() throws Exception {
        LOG.info("Starting pulsar message queue reader service: {}", name);

        this.client = PulsarClient.builder()
                .serviceUrl(serviceUrl)
                .build();
        this.consumer = client.newConsumer(Schema.BYTES)
                .topic(topic)
                .subscriptionName(name)
                .intercept(new MessageInterceptor())
                // TODO Tweak?
                .batchReceivePolicy(BatchReceivePolicy.DEFAULT_POLICY)
                .subscribe();

        processBuffer = processBufferProvider.get();
        // Service is ready for consuming
        latch.countDown();
    }

    @Override
    protected void shutDown() throws Exception {
        if (consumer != null) {
            consumer.close();
        }
        if (client != null) {
            client.close();
            client.shutdown();
        }
    }

    @Override
    protected void run() throws Exception {
        // TODO pause processing when LifeCycle changes
        // TODO add metrics
        // TODO the JournalReader limits the read based on the remaining capacity in the processBuffer
        //      do we need this?   final long remainingCapacity = processBuffer.getRemainingCapacity();

        while (isRunning()) {
            final List<MessageQueue.Entry> entries = read();
            entries.forEach(entry -> {
                LOG.info("Consumed message: {}", entry);
                final RawMessage rawMessage = RawMessage.decode(entry.value(), entry.commitId());
                // TODO: we need to interrupt the execution thread on shutdown to not inhibit server shutdown when
                //  the buffer is full
                processBuffer.insertBlocking(rawMessage);
            });
        }
    }

    private List<MessageQueue.Entry> read() throws MessageQueueException {
        final ImmutableList.Builder<MessageQueue.Entry> builder = ImmutableList.builder();

        if (!isRunning()) {
            throw new MessageQueueException("Message queue service is not running");
        }

        try {
            final Messages<byte[]> messages = consumer.batchReceive();
            // TODO use converter
            messages.forEach(message -> {
                builder.add(PulsarMessageQueueEntry.fromMessage(message));
            });
        } catch (PulsarClientException e) {
            throw new MessageQueueException("Error consuming messages", e);
        }

        return builder.build();
    }

    public void commit(Object messageId) {
        if (messageId instanceof MessageId) {
            try {
                consumer.acknowledge((MessageId) messageId);
            } catch (PulsarClientException e) {
                LOG.error("Couldn't acknowledge message", e);
            }
        } else {
            LOG.error("Couldn't acknowledge message. Expected <" + messageId + "> to be a MessageId");
        }
    }

    private class MessageInterceptor implements ConsumerInterceptor<byte[]> {
        @Override
        public void close() {

        }

        @Override
        public Message<byte[]> beforeConsume(org.apache.pulsar.client.api.Consumer<byte[]> consumer, Message<byte[]> message) {
            final int length = message.getData().length;

            messageMeter.mark();
            byteCounter.inc(length);
            byteMeter.mark(length);

            return message;
        }

        @Override
        public void onAcknowledge(org.apache.pulsar.client.api.Consumer<byte[]> consumer, MessageId messageId, Throwable exception) {

        }

        @Override
        public void onAcknowledgeCumulative(org.apache.pulsar.client.api.Consumer<byte[]> consumer, MessageId messageId, Throwable exception) {

        }

        @Override
        public void onNegativeAcksSend(org.apache.pulsar.client.api.Consumer<byte[]> consumer, Set<MessageId> messageIds) {

        }

        @Override
        public void onAckTimeoutSend(org.apache.pulsar.client.api.Consumer<byte[]> consumer, Set<MessageId> messageIds) {

        }
    }
}
