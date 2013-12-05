/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
 *
 */
package org.graylog2.inputs.kafka;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import kafka.consumer.*;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;
import org.graylog2.plugin.InputHost;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.RadioMessage;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.joda.time.DateTime;
import org.msgpack.MessagePack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class KafkaInput extends MessageInput {

    // Kaefer.

    private static final Logger LOG = LoggerFactory.getLogger(KafkaInput.class);

    public static final String NAME = "Kafka Input";

    private ConsumerConnector cc;

    private boolean stopped = false;

    private AtomicLong totalBytesRead = new AtomicLong(0);
    private AtomicLong lastSecBytesRead = new AtomicLong(0);
    private AtomicLong lastSecBytesReadTmp = new AtomicLong(0);

    @Override
    public void checkConfiguration() throws ConfigurationException {
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
    public void launch() throws MisfireException {
        setupMetrics();

        Properties props = new Properties();

        props.put("group.id", GROUP_ID);
        props.put("client.id", "gl2-" + graylogServer.getNodeId() + "-" + getId());

        props.put("fetch.min.bytes", String.valueOf(configuration.getInt(CK_FETCH_MIN_BYTES)));
        props.put("fetch.wait.max.ms", String.valueOf(configuration.getInt(CK_FETCH_WAIT_MAX)));
        props.put("zookeeper.connect", configuration.getString(CK_ZOOKEEPER));

        ConsumerConfig consumerConfig = new ConsumerConfig(props);
        cc = Consumer.createJavaConsumerConnector(consumerConfig);

        TopicFilter filter = new Whitelist(configuration.getString(CK_TOPIC_FILTER));

        List<KafkaStream<byte[], byte[]>> streams = cc.createMessageStreamsByFilter(filter, (int) configuration.getInt(CK_THREADS));
        ExecutorService executor = Executors.newFixedThreadPool(5);

        final MessageInput thisInput = this;

        for (final KafkaStream<byte[], byte[]> stream : streams) {
            executor.submit(new Runnable() {
                public void run() {
                    MessagePack msgpack = new MessagePack();

                    for(MessageAndMetadata message : stream) {
                        if (stopped) {
                            return;
                        }

                        try {
                            byte[] bytes = (byte[]) message.message();

                            totalBytesRead.addAndGet(bytes.length);
                            lastSecBytesReadTmp.addAndGet(bytes.length);

                            RadioMessage msg = msgpack.read(bytes, RadioMessage.class);

                            if (!msg.strings.containsKey("message") || !msg.strings.containsKey("source") || msg.timestamp <= 0) {
                                LOG.error("Incomplete radio message. Skipping.");
                                continue;
                            }

                            Message event = new Message(
                                    msg.strings.get("message"),
                                    msg.strings.get("source"),
                                    new DateTime(msg.timestamp)
                            );

                            event.addStringFields(msg.strings);
                            event.addIntegerFields(msg.ints);

                            graylogServer.getProcessBuffer().insertCached(event, thisInput);
                        } catch (Exception e) {
                            LOG.error("Error while trying to process Kafka message.", e);
                            continue;
                        }
                    }
                }
            });
        }

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                lastSecBytesRead.set(lastSecBytesReadTmp.get());
                lastSecBytesReadTmp.set(0);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        stopped = true;

        if (cc != null) {
            cc.shutdown();
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
                10,
                "Number of processor threads to spawn.",
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
        graylogServer.metrics().register(MetricRegistry.name(getUniqueReadableId(), "read_bytes_1sec"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                // TODO
                return lastSecBytesRead.get();
            }
        });

        graylogServer.metrics().register(MetricRegistry.name(getUniqueReadableId(), "written_bytes_1sec"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                // TODO
                return 0L;
            }
        });

        graylogServer.metrics().register(MetricRegistry.name(getUniqueReadableId(), "read_bytes_total"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                return totalBytesRead.get();
            }
        });

        graylogServer.metrics().register(MetricRegistry.name(getUniqueReadableId(), "written_bytes_total"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                // TODO
                return 0L;
            }
        });

    }

}
