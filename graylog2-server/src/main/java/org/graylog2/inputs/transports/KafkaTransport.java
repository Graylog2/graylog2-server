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
package org.graylog2.inputs.transports;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.InvalidOffsetException;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.internals.NoOpConsumerRebalanceListener;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.graylog.shaded.kafka09.consumer.Consumer;
import org.graylog.shaded.kafka09.consumer.ConsumerConfig;
import org.graylog.shaded.kafka09.consumer.ConsumerIterator;
import org.graylog.shaded.kafka09.consumer.ConsumerTimeoutException;
import org.graylog.shaded.kafka09.consumer.KafkaStream;
import org.graylog.shaded.kafka09.consumer.TopicFilter;
import org.graylog.shaded.kafka09.consumer.Whitelist;
import org.graylog.shaded.kafka09.javaapi.consumer.ConsumerConnector;
import org.graylog.shaded.kafka09.message.MessageAndMetadata;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.transports.ThrottleableTransport;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static com.codahale.metrics.MetricRegistry.name;

public class KafkaTransport extends ThrottleableTransport {
    public static final String CK_LEGACY = "legacy_mode";
    public static final String CK_FETCH_MIN_BYTES = "fetch_min_bytes";
    public static final String CK_FETCH_WAIT_MAX = "fetch_wait_max";
    public static final String CK_ZOOKEEPER = "zookeeper";
    public static final String CK_BOOTSTRAP = "bootstrap_server";
    public static final String CK_TOPIC_FILTER = "topic_filter";
    public static final String CK_THREADS = "threads";
    public static final String CK_OFFSET_RESET = "offset_reset";
    public static final String CK_GROUP_ID = "group_id";
    public static final String CK_CUSTOM_PROPERTIES = "custom_properties";

    // See https://kafka.apache.org/090/documentation.html for available values for "auto.offset.reset".
    private static final ImmutableMap<String, String> OFFSET_RESET_VALUES = ImmutableMap.of(
            "largest", "Automatically reset the offset to the latest offset", // "largest" OR "latest"
            "smallest", "Automatically reset the offset to the earliest offset" // "smallest" OR "earliest"
    );

    private static final String DEFAULT_OFFSET_RESET = "largest";
    private static final String DEFAULT_GROUP_ID = "graylog2";

    private static final Logger LOG = LoggerFactory.getLogger(KafkaTransport.class);

    private final Configuration configuration;
    private final MetricRegistry localRegistry;
    private final NodeId nodeId;
    private final EventBus serverEventBus;
    private final ServerStatus serverStatus;
    private final ScheduledExecutorService scheduler;
    private final MetricRegistry metricRegistry;
    private final AtomicLong totalBytesRead = new AtomicLong(0);
    private final AtomicLong lastSecBytesRead = new AtomicLong(0);
    private final AtomicLong lastSecBytesReadTmp = new AtomicLong(0);
    private final ExecutorService executor;

    private volatile boolean stopped = false;
    private volatile boolean paused = true;
    private volatile CountDownLatch pausedLatch = new CountDownLatch(1);

    private CountDownLatch stopLatch;
    private ConsumerConnector cc;

    @AssistedInject
    public KafkaTransport(@Assisted Configuration configuration,
                          LocalMetricRegistry localRegistry,
                          NodeId nodeId,
                          EventBus serverEventBus,
                          ServerStatus serverStatus,
                          @Named("daemonScheduler") ScheduledExecutorService scheduler) {
        super(serverEventBus, configuration);
        this.configuration = configuration;
        this.localRegistry = localRegistry;
        this.nodeId = nodeId;
        this.serverEventBus = serverEventBus;
        this.serverStatus = serverStatus;
        this.scheduler = scheduler;
        this.metricRegistry = localRegistry;
        final int numThreads = configuration.getInt(CK_THREADS);
        this.executor = executorService(numThreads);

        localRegistry.register("read_bytes_1sec", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return lastSecBytesRead.get();
            }
        });
        localRegistry.register("written_bytes_1sec", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return 0L;
            }
        });
        localRegistry.register("read_bytes_total", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return totalBytesRead.get();
            }
        });
        localRegistry.register("written_bytes_total", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return 0L;
            }
        });
    }

    @Subscribe
    public void lifecycleStateChange(Lifecycle lifecycle) {
        LOG.debug("Lifecycle changed to {}", lifecycle);
        switch (lifecycle) {
            case PAUSED:
            case FAILED:
            case HALTING:
                pausedLatch = new CountDownLatch(1);
                paused = true;
                break;
            default:
                paused = false;
                pausedLatch.countDown();
                break;
        }
    }

    @Override
    public void setMessageAggregator(CodecAggregator ignored) {
    }

    @Override
    public void doLaunch(final MessageInput input) {
        final boolean legacyMode = configuration.getBoolean(CK_LEGACY, true);
        if (legacyMode) {
            final String zooKeper = configuration.getString(CK_ZOOKEEPER);
            if (Strings.isNullOrEmpty(zooKeper)) {
                throw new IllegalArgumentException("ZooKeeper configuration setting cannot be empty");
            }
        } else {
            final String bootStrap = configuration.getString(CK_BOOTSTRAP);
            if (Strings.isNullOrEmpty(bootStrap)) {
                throw new IllegalArgumentException("Bootstrap server configuration setting cannot be empty");
            }
        }

        serverStatus.awaitRunning(() -> lifecycleStateChange(Lifecycle.RUNNING));
        // listen for lifecycle changes
        serverEventBus.register(this);

        if (legacyMode) {
            doLaunchLegacy(input);
        } else {
            doLaunchConsumer(input);
        }
        scheduler.scheduleAtFixedRate(() -> lastSecBytesRead.set(lastSecBytesReadTmp.getAndSet(0)), 1, 1, TimeUnit.SECONDS);
    }

    private void doLaunchConsumer(final MessageInput input) {
        final Properties props = new Properties();

        props.put("group.id", configuration.getString(CK_GROUP_ID, DEFAULT_GROUP_ID));
        props.put("fetch.min.bytes", String.valueOf(configuration.getInt(CK_FETCH_MIN_BYTES)));
        props.put("fetch.max.wait.ms", String.valueOf(configuration.getInt(CK_FETCH_WAIT_MAX)));
        //noinspection ConstantConditions
        props.put("bootstrap.servers", configuration.getString(CK_BOOTSTRAP));
        // Map largest -> latest, smallest -> earliest
        final String resetValue = configuration.getString(CK_OFFSET_RESET, DEFAULT_OFFSET_RESET);
        props.put("auto.offset.reset", resetValue.equals("largest") ? "latest" : "earliest");
        // Default auto commit interval is 60 seconds. Reduce to 1 second to minimize message duplication
        // if something breaks.
        props.put("auto.commit.interval.ms", "1000");
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());

        insertCustomProperties(props);

        final int numThreads = configuration.getInt(CK_THREADS);
        // this is being used during shutdown to first stop all submitted jobs before committing the offsets back to zookeeper
        // and then shutting down the connection.
        // this is to avoid yanking away the connection from the consumer runnables
        stopLatch = new CountDownLatch(numThreads);

        IntStream.range(0, numThreads).forEach(i -> executor.submit(new ConsumerRunnable(props, input, i)));
    }

    private class ConsumerRunnable implements Runnable {
        private final MessageInput input;
        private final KafkaConsumer<byte[], byte[]> consumer;

        public ConsumerRunnable(Properties props, MessageInput input, int threadId) {
            this.input = input;
            final Properties nprops = (Properties) props.clone();
            nprops.put("client.id", "gl2-" + nodeId.getShortNodeId() + "-" + input.getId() + "-" + threadId);
            consumer = new KafkaConsumer<>(nprops);
            //noinspection ConstantConditions
            consumer.subscribe(Pattern.compile(configuration.getString(CK_TOPIC_FILTER)), new NoOpConsumerRebalanceListener());
        }

        private void consumeRecords(ConsumerRecords<byte[], byte[]> consumerRecords) {
            for (final ConsumerRecord<byte[], byte[]> record : consumerRecords) {
                if (paused) {
                    // we try not to spin here, so we wait until the lifecycle goes back to running.
                    LOG.debug("Message processing is paused, blocking until message processing is turned back on.");
                    Uninterruptibles.awaitUninterruptibly(pausedLatch);
                }
                // check for being stopped before actually getting the message, otherwise we could end up losing that message
                if (stopped) {
                    break;
                }
                if (isThrottled()) {
                    blockUntilUnthrottled();
                }

                // process the message, this will immediately mark the message as having been processed. this gets tricky
                // if we get an exception about processing it down below.
                final byte[] bytes = record.value();

                // it is possible that the message is null
                if (bytes == null) {
                    continue;
                }
                totalBytesRead.addAndGet(bytes.length);
                lastSecBytesReadTmp.addAndGet(bytes.length);

                final RawMessage rawMessage = new RawMessage(bytes);
                input.processRawMessage(rawMessage);
            }
        }

        private Optional<ConsumerRecords<byte[], byte[]>> tryPoll() {
            try {
                // Workaround https://issues.apache.org/jira/browse/KAFKA-4189 by calling wakeup()
                final ScheduledFuture<?> future = scheduler.schedule(consumer::wakeup, 2000, TimeUnit.MILLISECONDS);
                final ConsumerRecords<byte[], byte[]> consumerRecords = consumer.poll(1000);
                future.cancel(true);

                return Optional.of(consumerRecords);
            } catch (WakeupException e) {
                LOG.error("WakeupException in poll. Kafka server is not responding.");
            } catch (InvalidOffsetException | AuthorizationException e) {
                LOG.error("Exception in poll.", e);
            }
            return Optional.empty();
        }

        @Override
        public void run() {
            while (!stopped) {
                final Optional<ConsumerRecords<byte[], byte[]>> consumerRecords;
                try {
                    consumerRecords = tryPoll();
                    if (! consumerRecords.isPresent()) {
                        LOG.error("Caught recoverable exception. Retrying");
                        Thread.sleep(2000);
                        continue;
                    }
                } catch (KafkaException | InterruptedException e) {
                    LOG.error("Caught unrecoverable exception in poll. Stopping input", e);
                    stopped = true;
                    break;
                }
                try {
                    consumeRecords(consumerRecords.get());
                } catch (Exception e) {
                    LOG.error("Exception in consumer thread. Stopping input", e);
                    stopped = true;
                    break;
                }
            }
            // explicitly commit our offsets when stopping.
            // this might trigger a couple of times, but it won't hurt
            consumer.commitAsync();
            stopLatch.countDown();
            // TODO once we update our kafka client, we should call this with a timeout
            // Otherwise might hang if kafka is not available: https://issues.apache.org/jira/browse/KAFKA-3822
            consumer.close();
        }
    }

    private void doLaunchLegacy(final MessageInput input) {
        final Properties props = new Properties();

        props.put("group.id", configuration.getString(CK_GROUP_ID, DEFAULT_GROUP_ID));
        props.put("client.id", "gl2-" + nodeId.getShortNodeId() + "-" + input.getId());

        props.put("fetch.min.bytes", String.valueOf(configuration.getInt(CK_FETCH_MIN_BYTES)));
        props.put("fetch.wait.max.ms", String.valueOf(configuration.getInt(CK_FETCH_WAIT_MAX)));
        props.put("zookeeper.connect", configuration.getString(CK_ZOOKEEPER));
        props.put("auto.offset.reset", configuration.getString(CK_OFFSET_RESET, DEFAULT_OFFSET_RESET));
        // Default auto commit interval is 60 seconds. Reduce to 1 second to minimize message duplication
        // if something breaks.
        props.put("auto.commit.interval.ms", "1000");
        // Set a consumer timeout to avoid blocking on the consumer iterator.
        props.put("consumer.timeout.ms", "1000");

        insertCustomProperties(props);

        final int numThreads = configuration.getInt(CK_THREADS);
        final ConsumerConfig consumerConfig = new ConsumerConfig(props);
        cc = Consumer.createJavaConsumerConnector(consumerConfig);

        final TopicFilter filter = new Whitelist(configuration.getString(CK_TOPIC_FILTER));

        final List<KafkaStream<byte[], byte[]>> streams = cc.createMessageStreamsByFilter(filter, numThreads);

        // this is being used during shutdown to first stop all submitted jobs before committing the offsets back to zookeeper
        // and then shutting down the connection.
        // this is to avoid yanking away the connection from the consumer runnables
        stopLatch = new CountDownLatch(streams.size());

        for (final KafkaStream<byte[], byte[]> stream : streams) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    final ConsumerIterator<byte[], byte[]> consumerIterator = stream.iterator();
                    boolean retry;

                    do {
                        retry = false;

                        try {
                            // we have to use hasNext() here instead foreach, because next() marks the message as processed immediately
                            // noinspection WhileLoopReplaceableByForEach
                            while (consumerIterator.hasNext()) {
                                if (paused) {
                                    // we try not to spin here, so we wait until the lifecycle goes back to running.
                                    LOG.debug(
                                            "Message processing is paused, blocking until message processing is turned back on.");
                                    Uninterruptibles.awaitUninterruptibly(pausedLatch);
                                }
                                // check for being stopped before actually getting the message, otherwise we could end up losing that message
                                if (stopped) {
                                    break;
                                }
                                if (isThrottled()) {
                                    blockUntilUnthrottled();
                                }

                                // process the message, this will immediately mark the message as having been processed. this gets tricky
                                // if we get an exception about processing it down below.
                                final MessageAndMetadata<byte[], byte[]> message = consumerIterator.next();

                                final byte[] bytes = message.message();

                                // it is possible that the message is null
                                if (bytes == null) {
                                    continue;
                                }

                                totalBytesRead.addAndGet(bytes.length);
                                lastSecBytesReadTmp.addAndGet(bytes.length);

                                final RawMessage rawMessage = new RawMessage(bytes);

                                input.processRawMessage(rawMessage);
                            }
                        } catch (ConsumerTimeoutException e) {
                            // Happens when there is nothing to consume, retry to check again.
                            retry = true;
                        } catch (Exception e) {
                            LOG.error("Kafka consumer error, stopping consumer thread.", e);
                        }
                    } while (retry && !stopped);
                    // explicitly commit our offsets when stopping.
                    // this might trigger a couple of times, but it won't hurt
                    cc.commitOffsets();
                    stopLatch.countDown();
                }
            });
        }
    }

    private void insertCustomProperties(Properties props) {
        try {
            final Properties customProperties = new Properties();
            customProperties.load(new ByteArrayInputStream(configuration.getString(CK_CUSTOM_PROPERTIES, "").getBytes(StandardCharsets.UTF_8)));
            props.putAll(customProperties);
        } catch (IOException e) {
            LOG.error("Failed to read custom properties", e);
        }
    }

    private ExecutorService executorService(int numThreads) {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("kafka-transport-%d").build();
        return new InstrumentedExecutorService(
                Executors.newFixedThreadPool(numThreads, threadFactory),
                metricRegistry,
                name(this.getClass(), "executor-service"));
    }

    @Override
    public void doStop() {
        stopped = true;

        serverEventBus.unregister(this);

        if (stopLatch != null) {
            try {
                // unpause the processors if they are blocked. this will cause them to see that we are stopping, even if they were paused.
                if (pausedLatch != null && pausedLatch.getCount() > 0) {
                    pausedLatch.countDown();
                }
                final boolean allStoppedOrderly = stopLatch.await(5, TimeUnit.SECONDS);
                stopLatch = null;
                if (!allStoppedOrderly) {
                    // timed out
                    LOG.info(
                            "Stopping Kafka input timed out (waited 5 seconds for consumer threads to stop). Forcefully closing connection now. " +
                                    "This is usually harmless when stopping the input.");
                }
            } catch (InterruptedException e) {
                LOG.debug("Interrupted while waiting to stop input.");
            }
        }
        if (cc != null) {
            cc.shutdown();
            cc = null;
        }
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.error("Interrupted in transport executor shutdown.");
        }
    }

    @Override
    public MetricSet getMetricSet() {
        return localRegistry;
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<KafkaTransport> {
        @Override
        KafkaTransport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends ThrottleableTransport.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest cr = super.getRequestedConfiguration();

            cr.addField(new BooleanField(CK_LEGACY,
                    "Legacy mode",
                    true,
                    "Use old ZooKeeper-based consumer API. (Used before Graylog 3.3)",
                    10
            ));
            cr.addField(new TextField(
                    CK_BOOTSTRAP,
                    "Bootstrap Servers",
                    "127.0.0.1:9092",
                    "Comma separated list of one or more Kafka brokers. (Format: \"host1:port1,host2:port2\")." +
                            "Not used in legacy mode.",
                    ConfigurationField.Optional.OPTIONAL,
                    11));
            cr.addField(new TextField(
                    CK_ZOOKEEPER,
                    "ZooKeeper address (legacy mode only)",
                    "127.0.0.1:2181",
                    "Host and port of the ZooKeeper that is managing your Kafka cluster. Not used in consumer API (non-legacy) mode.",
                    ConfigurationField.Optional.OPTIONAL,
                    12));
            cr.addField(new TextField(
                    CK_TOPIC_FILTER,
                    "Topic filter regex",
                    "^your-topic$",
                    "Every topic that matches this regular expression will be consumed.",
                    ConfigurationField.Optional.NOT_OPTIONAL));

            cr.addField(new NumberField(
                    CK_FETCH_MIN_BYTES,
                    "Fetch minimum bytes",
                    5,
                    "Wait for a message batch to reach at least this size or the configured maximum wait time before fetching.",
                    ConfigurationField.Optional.NOT_OPTIONAL));

            cr.addField(new NumberField(
                    CK_FETCH_WAIT_MAX,
                    "Fetch maximum wait time (ms)",
                    100,
                    "Wait for this time or the configured minimum size of a message batch before fetching.",
                    ConfigurationField.Optional.NOT_OPTIONAL));

            cr.addField(new NumberField(
                    CK_THREADS,
                    "Processor threads",
                    2,
                    "Number of processor threads to spawn. Use one thread per Kafka topic partition.",
                    ConfigurationField.Optional.NOT_OPTIONAL));

            cr.addField(new DropdownField(
                    CK_OFFSET_RESET,
                    "Auto offset reset",
                    DEFAULT_OFFSET_RESET,
                    OFFSET_RESET_VALUES,
                    "What to do when there is no initial offset in Kafka or if an offset is out of range",
                    ConfigurationField.Optional.OPTIONAL));

            cr.addField(new TextField(
                    CK_GROUP_ID,
                    "Consumer group id",
                    DEFAULT_GROUP_ID,
                    "Name of the consumer group the Kafka input belongs to",
                    ConfigurationField.Optional.OPTIONAL));
            cr.addField(new TextField(
                    CK_CUSTOM_PROPERTIES,
                    "Custom Kafka properties",
                    "",
                    "A newline separated list of Kafka properties. (e.g.: \"ssl.keystore.location=/etc/graylog/server/kafka.keystore.jks\").",
                    ConfigurationField.Optional.OPTIONAL,
                    ConfigurationField.PLACE_AT_END_POSITION,
                    TextField.Attribute.TEXTAREA
                    ));

            return cr;
        }
    }
}

