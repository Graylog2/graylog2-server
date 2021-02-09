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
package org.graylog2.shared.messageq.kafka;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.Uninterruptibles;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.buffers.ProcessBuffer;
import org.graylog2.shared.messageq.AbstractMessageQueueReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Singleton
public class KafkaMessageQueueReader extends AbstractMessageQueueReader {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaMessageQueueReader.class);

    private final CountDownLatch latch = new CountDownLatch(1);
    private final Timer ackTimer;

    private Consumer<String, byte[]> consumer;
    private Provider<ProcessBuffer> processBufferProvider;
    private ProcessBuffer processBuffer;
    private final Properties props;
    private ConcurrentHashMap<TopicPartition, Long> commitableOffsets;

    @Inject
    public KafkaMessageQueueReader(MetricRegistry metricRegistry,
                                   Provider<ProcessBuffer> processBufferProvider,
                                   EventBus eventBus) {
        super(eventBus);

        // Using a ProcessBuffer directly will lead to guice error:
        // "Please wait until after injection has completed to use this object."
        this.processBufferProvider = processBufferProvider;

        props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("client.id", "node-id"); // TODO what to use?
        props.put("group.id", "test"); // TODO what to use?
        props.put("enable.auto.commit", "false");
        props.put("session.timeout.ms", "30000");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        props.put("auto.offset.reset", "earliest");
        // max.poll.records

        commitableOffsets = new ConcurrentHashMap<>();

        this.ackTimer = metricRegistry.timer(name(this.getClass(), "ackTime"));
    }

    @Override
    protected void startUp() throws Exception {
        super.startUp();

        LOG.info("Starting Kafka message queue reader service");

        this.consumer = new KafkaConsumer<>(props);
        consumer.subscribe(ImmutableList.of("message-input"));

        processBuffer = processBufferProvider.get();
        // Service is ready for consuming
        latch.countDown();
    }

    @Override
    protected void shutDown() throws Exception {
        if (consumer != null) {
            consumer.close();
        }
        super.shutDown();
    }

    @Override
    protected void run() throws Exception {
        // TODO add metrics
        // TODO the JournalReader limits the read based on the remaining capacity in the processBuffer
        //      do we need this?   final long remainingCapacity = processBuffer.getRemainingCapacity();

        while (isRunning()) {
            if (!shouldBeReading()) {
                Uninterruptibles.sleepUninterruptibly(100, MILLISECONDS);
                continue;
            }
            final ConsumerRecords<String, byte[]> consumerRecords = read();
            consumerRecords.forEach(record -> {
                LOG.debug("Consumed message: {}", record);
                final RawMessage rawMessage = RawMessage.decode(record.value(), KafkaMessageQueueEntry.CommitId.fromRecord(record));
                // FIXME: on a full process buffer, where not a single entry is ever taken out again, this call will
                //  block (obviously) forever. But it can't even be unblocked by interrupting the thread, e.g. for
                //  shutting down the server. This will inhibit server shutdown when elastic search is down.
                processBuffer.insertBlocking(rawMessage);
            });
            doCommit();
        }
    }

    private ConsumerRecords<String, byte[]> read() {
        synchronized (consumer) {
            final ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(50));
            return records;
        }
    }

    private void doCommit() {
        if (commitableOffsets.isEmpty()) {
            return;
        }

        final ConcurrentHashMap<TopicPartition, Long> copy = this.commitableOffsets;
        this.commitableOffsets = new ConcurrentHashMap<>();

        final Map<TopicPartition, OffsetAndMetadata> commitMap = copy.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> new OffsetAndMetadata(v.getValue() + 1)));
        try (Timer.Context ignored = ackTimer.time()){
            consumer.commitSync(commitMap);
        }
    }

    // TODO
    //    public void commit(List<Object> messageIds) {
    //        messageIds.stream().filter(KafkaMessageQueueEntry.CommitId.class::isInstance).map(e -> {
    //            final KafkaMessageQueueEntry.CommitId commitId = (KafkaMessageQueueEntry.CommitId) e;
    //        })
    //    }

    public void commit(Object object) {
        if (object instanceof KafkaMessageQueueEntry.CommitId) {
            final KafkaMessageQueueEntry.CommitId commitId = (KafkaMessageQueueEntry.CommitId) object;

            commitableOffsets.compute(commitId.getTopicPartition(), (partition, offset) -> {
                if (offset == null) {
                    return commitId.getOffset();
                }
                if (offset >= commitId.getOffset()) {
                    return offset;
                }
                return commitId.getOffset();
            });
        } else {
            LOG.error("Couldn't acknowledge message. Expected <" + object + "> to be a KafkaMessageQueueEntry.CommitId");
        }
    }

}
