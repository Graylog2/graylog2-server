/*
 * Copyright 2012-2014 TORCH GmbH
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.inputs.kafka;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.inject.Inject;
import kafka.consumer.*;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.RadioMessage;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
import org.graylog2.plugin.buffers.ProcessingDisabledException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.msgpack.MessagePack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class KafkaInput extends MessageInput {

    // Kaefer.

    private static final Logger LOG = LoggerFactory.getLogger(KafkaInput.class);

    public static final String NAME = "Kafka Input";
    private final MetricRegistry metricRegistry;
    private final NodeId nodeId;
    private final EventBus serverEventBus;

    private ConsumerConnector cc;

    private volatile boolean stopped = false;
    private volatile boolean paused = true;
    private CountDownLatch pausedLatch = new CountDownLatch(1);

    private AtomicLong totalBytesRead = new AtomicLong(0);
    private AtomicLong lastSecBytesRead = new AtomicLong(0);
    private AtomicLong lastSecBytesReadTmp = new AtomicLong(0);

    private CountDownLatch stopLatch;

    @Inject
    public KafkaInput(MetricRegistry metricRegistry,
                      NodeId nodeId,
                      EventBus serverEventBus) {
        this.metricRegistry = metricRegistry;
        this.nodeId = nodeId;
        this.serverEventBus = serverEventBus;
    }

    @Subscribe
    public void lifecycleStateChange(Lifecycle lifecycle) {
        LOG.debug("Lifecycle changed to {}", lifecycle);
        switch (lifecycle) {
            case RUNNING:
                paused = false;
                pausedLatch.countDown();
                break;
            default:
                pausedLatch = new CountDownLatch(1);
                paused = true;
        }
    }

    @Override
    public void checkConfiguration(Configuration configuration) throws ConfigurationException {
        if (!checkConfig(configuration)) {
            throw new ConfigurationException(configuration.getSource().toString());
        }
    }

    public static final String GROUP_ID = "graylog2";

    public static final String CK_FETCH_MIN_BYTES = "fetch_min_bytes";
    public static final String CK_FETCH_WAIT_MAX = "fetch_wait_max";
    public static final String CK_ZOOKEEPER = "zookeeper";
    public static final String CK_TOPIC_FILTER = "topic_filter";
    public static final String CK_THREADS = "threads";

    @Override
    public void initialize(Configuration configuration) {
        super.initialize(configuration);

        setupMetrics();
    }

    @Override
    public void launch(final Buffer processBuffer) throws MisfireException {
        // listen for lifecycle changes
        serverEventBus.register(this);

        Properties props = new Properties();

        props.put("group.id", GROUP_ID);
        props.put("client.id", "gl2-" + nodeId + "-" + getId());

        props.put("fetch.min.bytes", String.valueOf(configuration.getInt(CK_FETCH_MIN_BYTES)));
        props.put("fetch.wait.max.ms", String.valueOf(configuration.getInt(CK_FETCH_WAIT_MAX)));
        props.put("zookeeper.connect", configuration.getString(CK_ZOOKEEPER));
        // Default auto commit interval is 60 seconds. Reduce to 1 second to minimize message duplication
        // if something breaks.
        props.put("auto.commit.interval.ms", "1000");

        final int numThreads = (int) configuration.getInt(CK_THREADS);
        ConsumerConfig consumerConfig = new ConsumerConfig(props);
        cc = Consumer.createJavaConsumerConnector(consumerConfig);

        TopicFilter filter = new Whitelist(configuration.getString(CK_TOPIC_FILTER));

        List<KafkaStream<byte[], byte[]>> streams = cc.createMessageStreamsByFilter(filter, numThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        final MessageInput thisInput = this;

        // this is being used during shutdown to first stop all submitted jobs before committing the offsets back to zookeeper
        // and then shutting down the connection.
        // this is to avoid yanking away the connection from the consumer runnables
        stopLatch = new CountDownLatch(streams.size());

        for (final KafkaStream<byte[], byte[]> stream : streams) {
            executor.submit(new Runnable() {
                public void run() {
                    MessagePack msgpack = new MessagePack();

                    ConsumerIterator<byte[], byte[]> consumerIterator = stream.iterator();

                    // we have to use hasNext() here instead foreach, because next() marks the message as processed immediately
                    while (consumerIterator.hasNext()) {
                        if (paused) {
                            // we try not to spin here, so we wait until the lifecycle goes back to running.
                            LOG.debug("Message processing is paused, blocking until message processing is turned back on.");
                            Uninterruptibles.awaitUninterruptibly(pausedLatch);
                        }
                        // check for being stopped before actually getting the message, otherwise we could end up losing that message
                        if (stopped) {
                            break;
                        }

                        // process the message, this will immediately mark the message as having been processed. this gets tricky
                        // if we get an exception about processing it down below.
                        final MessageAndMetadata<byte[], byte[]> message = consumerIterator.next();
                        final Message event = decodeMessage(msgpack, message);
                        if (event == null) return; // TODO should this actually return?

                        // the loop below is like this because we cannot "unsee" the message we've just gotten by calling .next()
                        // the high level consumer of Kafka marks the message as "processed" immediately after being returned from next.
                        // thus we need to retry processing it.
                        boolean retry = false;
                        int retryCount = 0;
                        do {
                            try {
                                if (retry) {
                                    // don't try immediately if the buffer was full, try not spin too much
                                    LOG.debug("Waiting 10ms to retry inserting into buffer, retried {} times", retryCount);
                                    Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MILLISECONDS); // TODO magic number
                                }
                                // try to process the message, if it succeeds, we immediately move on to the next message (retry will be false)
                                processBuffer.insertFailFast(event, thisInput);
                                retry = false;
                            } catch (BufferOutOfCapacityException e) {
                                LOG.debug("Input buffer full, retrying Kafka message processing");
                                retry = true;
                                retryCount++;
                            } catch (ProcessingDisabledException e) {
                                LOG.debug("Processing was disabled after we read the message but before we could insert it into " +
                                                  "the buffer. We cache this one message, and should block on the next iteration.");
                                processBuffer.insertCached(event, thisInput);
                                retry = false;
                            }
                        } while (retry);
                    }
                    // explicitly commit our offsets when stopping.
                    // this might trigger a couple of times, but it won't hurt
                    cc.commitOffsets();
                    stopLatch.countDown();
                }

                private Message decodeMessage(MessagePack msgpack,
                                              MessageAndMetadata<byte[], byte[]> message) {
                    try {
                        byte[] bytes = message.message();

                        totalBytesRead.addAndGet(bytes.length);
                        lastSecBytesReadTmp.addAndGet(bytes.length);

                        RadioMessage msg = msgpack.read(bytes, RadioMessage.class);

                        if (!msg.strings.containsKey("message") || !msg.strings.containsKey("source") || msg.timestamp <= 0) {
                            LOG.error("Incomplete radio message. Skipping.");
                            return null;
                        }

                        Message event = new Message(
                                msg.strings.get("message"),
                                msg.strings.get("source"),
                                new DateTime(msg.timestamp)
                        );

                        event.addStringFields(msg.strings);
                        event.addLongFields(msg.longs);
                        event.addDoubleFields(msg.doubles);
                        return event;
                    } catch (Exception e) {
                        LOG.error("Error while processing Kafka radio message", e);
                        return null;
                    }
                }

            });
        }

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                lastSecBytesRead.set(lastSecBytesReadTmp.getAndSet(0));
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        stopped = true;

        serverEventBus.unregister(this);

        if (stopLatch != null) {
            try {
                // unpause the processors if they are blocked. this will cause them to see that we are stopping, even if they were paused.
                if (pausedLatch != null && pausedLatch.getCount() > 0) {
                    pausedLatch.countDown();
                }
                boolean allStoppedOrderly = stopLatch.await(5, TimeUnit.SECONDS);
                stopLatch = null;
                if (!allStoppedOrderly) {
                    // timed out
                    LOG.info("Stopping Kafka input timed out (waited 5 seconds for consumer threads to stop). Forcefully closing connection now. " +
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
    }

    @Override
    public ConfigurationRequest getRequestedConfiguration() {
        ConfigurationRequest cr = new ConfigurationRequest();

        cr.addField(new TextField(
                CK_ZOOKEEPER,
                "ZooKeeper address",
                "192.168.1.1:2181",
                "Host and port of the ZooKeeper that is managing your Kafka cluster.",
                ConfigurationField.Optional.NOT_OPTIONAL
        ));

        cr.addField(new TextField(
                CK_TOPIC_FILTER,
                "Topic filter regex",
                "^your-topic$",
                "Every topic that matches this regular expression will be consumed.",
                ConfigurationField.Optional.NOT_OPTIONAL

        ));

        cr.addField(new NumberField(
                CK_FETCH_MIN_BYTES,
                "Fetch minimum bytes",
                5,
                "Wait for a message batch to reach at least this size or the configured maximum wait time before fetching.",
                ConfigurationField.Optional.NOT_OPTIONAL)
        );

        cr.addField(new NumberField(
                CK_FETCH_WAIT_MAX,
                "Fetch maximum wait time (ms)",
                100,
                "Wait for this time or the configured minimum size of a message batch before fetching.",
                ConfigurationField.Optional.NOT_OPTIONAL)
        );

        cr.addField(new NumberField(
                CK_THREADS,
                "Processor threads",
                2,
                "Number of processor threads to spawn. Use one thread per Kafka topic partition.",
                ConfigurationField.Optional.NOT_OPTIONAL)
        );

        return cr;
    }

    @Override
    public boolean isExclusive() {
        return false;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String linkToDocs() {
        return "http://support.torch.sh/help/kb/getting-your-logs-into-graylog2/using-the-kafka-message-input";
    }

    @Override
    public Map<String, Object> getAttributes() {
        return configuration.getSource();
    }

    protected boolean checkConfig(Configuration config) {
        return config.intIsSet(CK_FETCH_MIN_BYTES)
                && config.intIsSet(CK_FETCH_WAIT_MAX)
                && config.stringIsSet(CK_ZOOKEEPER)
                && config.stringIsSet(CK_TOPIC_FILTER)
                && config.intIsSet(CK_THREADS)  && config.getInt(CK_THREADS) > 0;
    }

    private void setupMetrics() {
        metricRegistry.register(MetricRegistry.name(getUniqueReadableId(), "read_bytes_1sec"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                // TODO
                return lastSecBytesRead.get();
            }
        });

        metricRegistry.register(MetricRegistry.name(getUniqueReadableId(), "written_bytes_1sec"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                // TODO
                return 0L;
            }
        });

        metricRegistry.register(MetricRegistry.name(getUniqueReadableId(), "read_bytes_total"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                return totalBytesRead.get();
            }
        });

        metricRegistry.register(MetricRegistry.name(getUniqueReadableId(), "written_bytes_total"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                // TODO
                return 0L;
            }
        });

    }

}
