/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.inputs.transports;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import kafka.consumer.ConsumerTimeoutException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static com.codahale.metrics.MetricRegistry.name;

public class KafkaTransport extends ThrottleableTransport {
    public static final String GROUP_ID = "graylog2";
    public static final String CK_FETCH_MIN_BYTES = "fetch_min_bytes";
    public static final String CK_FETCH_WAIT_MAX = "fetch_wait_max";
    public static final String CK_ZOOKEEPER = "zookeeper";
    public static final String CK_TOPIC_FILTER = "topic_filter";
    public static final String CK_THREADS = "threads";
    public static final String CK_OFFSET_RESET = "offset_reset";

    public static final String CK_BOOTSTRAP = "bootstrap_server";
    public static final String CK_SSL = "ssl";
    public static final String CK_SSL_KEYSTORE_LOCATION="ssl_keystore_location";
    public static final String CK_SSL_KEYSTORE_PASSWORD="ssl_keystore_password";
    public static final String CK_SSL_KEY_PASSWORD="ssl_key_password";
    public static final String CK_SSL_TRUSTSTORE_LOCATION="ssl_truststore_location";
    public static final String CK_SSL_TRUSTSTORE_PASSWORD="ssl_truststore_password";
    public static final String CK_SSL_ENABLED_PROTOCOL="ssl_enabled_protocol";


    // See https://kafka.apache.org/090/documentation.html for available values for "auto.offset.reset".
    private static final Map<String, String> OFFSET_RESET_VALUES = ImmutableMap.of(
            "latest", "Automatically reset the offset to the latest offset",
            "earliest", "Automatically reset the offset to the earliest offset"
    );

    private static final String DEFAULT_OFFSET_RESET = "latest";

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

    private volatile boolean stopped = false;
    private volatile boolean paused = true;
    private volatile CountDownLatch pausedLatch = new CountDownLatch(1);

    private CountDownLatch stopLatch;
    private KafkaStreams kstreams = null;

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
    public void doLaunch(final MessageInput input) throws MisfireException {
        serverStatus.awaitRunning(new Runnable() {
            @Override
            public void run() {
                lifecycleStateChange(Lifecycle.RUNNING);
            }
        });

        // listen for lifecycle changes
        serverEventBus.register(this);

        final Properties props = new Properties();

        props.put("group.id", GROUP_ID);
        props.put("client.id", "gl2-" + nodeId + "-" + input.getId());

        props.put("auto.offset.reset", configuration.getString(CK_OFFSET_RESET, DEFAULT_OFFSET_RESET));
        // Default auto commit interval is 60 seconds. Reduce to 1 second to minimize message duplication
        // if something breaks.
        props.put("commit.interval.ms", "1000");
        // Set a consumer timeout to avoid blocking on the consumer iterator.
        props.put("consumer.timeout.ms", "1000");
        props.put("bootstrap.servers", configuration.getString(CK_BOOTSTRAP));

        props.put(StreamsConfig.APPLICATION_ID_CONFIG, GROUP_ID);
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.ByteArray().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.ByteArray().getClass().getName());
        final int numThreads = configuration.getInt(CK_THREADS);
        props.put("num.stream.threads",numThreads);

        // SSL seettings

        props.put("ssl.keystore.location",configuration.getString(CK_SSL_KEYSTORE_LOCATION));
        props.put("ssl.keystore.password",configuration.getString(CK_SSL_KEYSTORE_PASSWORD));
        props.put("ssl.key.password",configuration.getString(CK_SSL_KEY_PASSWORD));
        props.put("ssl.truststore.location",configuration.getString(CK_SSL_TRUSTSTORE_LOCATION));
        props.put("ssl.truststore.password",configuration.getString(CK_SSL_TRUSTSTORE_PASSWORD));
        props.put("ssl.enabled.protocols",configuration.getString(CK_SSL_ENABLED_PROTOCOL));
        if(configuration.getString(CK_SSL).equals("true"))
            props.put("security.protocol","SSL");

        final StreamsBuilder builder = new StreamsBuilder();

        final List<KStream<byte[],byte[]>> streams = new ArrayList();

        AdminClient adminClient = AdminClient.create(props);

        try {

            adminClient.listTopics().names().get().stream()
                    .forEach(topicName->{
                        if(topicName.matches(configuration.getString(CK_TOPIC_FILTER))) {
                            streams.add(builder.stream(topicName));
                        }
                    });
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        finally{
            adminClient.close();
        }

        final ExecutorService executor = executorService(numThreads);

        // this is being used during shutdown to first stop all submitted jobs before committing the offsets back to zookeeper
        // and then shutting down the connection.
        // this is to avoid yanking away the connection from the consumer runnables
        stopLatch = new CountDownLatch(streams.size());

        for (final KStream<byte[], byte[]> stream : streams) {
            stream.foreach((keyBytes,messageBytes)-> {
                boolean retry;

                do {
                    retry = false;

                    try {
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

                        final byte[] bytes = messageBytes;
                        // it is possible that the message is null
                        if (bytes == null) {
                            continue;
                        }

                        totalBytesRead.addAndGet(bytes.length);
                        lastSecBytesReadTmp.addAndGet(bytes.length);

                        final RawMessage rawMessage = new RawMessage(bytes);

                        // TODO implement throttling
                        input.processRawMessage(rawMessage);
                    } catch (ConsumerTimeoutException e) {
                        // Happens when there is nothing to consume, retry to check again.
                        retry = true;
                    } catch (Exception e) {
                        LOG.error("Kafka consumer error, stopping consumer thread.", e);
                    }
                } while (retry && !stopped);

                stopLatch.countDown();

            });
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    kstreams = new KafkaStreams(builder.build(), props);

                    kstreams.cleanUp();
                    kstreams.start();
                }
            });
        }

        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                lastSecBytesRead.set(lastSecBytesReadTmp.getAndSet(0));
            }
        }, 1, 1, TimeUnit.SECONDS);
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
        if (kstreams != null) {
            kstreams.close();
            kstreams = null;
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

            cr.addField(new TextField(
                    CK_ZOOKEEPER,
                    "ZooKeeper address",
                    "127.0.0.1:2181",
                    "Host and port of the ZooKeeper that is managing your Kafka cluster.",
                    ConfigurationField.Optional.NOT_OPTIONAL));

            cr.addField(new TextField(
                    CK_BOOTSTRAP,
                    "Bootstrap Server",
                    "127.0.0.1:9092",
                    "Host and port of the Kafka Brokers , if ther are many comma seperated ",
                    ConfigurationField.Optional.NOT_OPTIONAL));

            cr.addField(new TextField(
                    CK_TOPIC_FILTER,
                    "Topic filter regex",
                    "^your-topic$",
                    "Every topic that matches this regular expression will be consumed.",
                    ConfigurationField.Optional.NOT_OPTIONAL));

            cr.addField(new TextField(
                    CK_SSL,
                    "SSL",
                    "true",
                    "true or false for SSL ",
                    ConfigurationField.Optional.NOT_OPTIONAL));

            cr.addField(new TextField(
                    CK_SSL_KEYSTORE_LOCATION,
                    "ssl keystore location",
                    "location",
                    "Path to the Physical location of Keystore file, required when SSL is true ",
                    ConfigurationField.Optional.OPTIONAL));

            cr.addField(new TextField(
                    CK_SSL_KEYSTORE_PASSWORD,
                    "ssl keystore password",
                    "password",
                    "password for Keystore file , required when SSL is true",
                    ConfigurationField.Optional.OPTIONAL));

            cr.addField(new TextField(
                    CK_SSL_KEY_PASSWORD,
                    "ssl key password",
                    "password",
                    "Key Password",
                    ConfigurationField.Optional.OPTIONAL));

            cr.addField(new TextField(
                    CK_SSL_TRUSTSTORE_LOCATION,
                    "ssl truststore location",
                    "location",
                    "Path to the physical location of truststore file, required when SSL is true",
                    ConfigurationField.Optional.OPTIONAL));

            cr.addField(new TextField(
                    CK_SSL_TRUSTSTORE_PASSWORD,
                    "ssl truststore password",
                    "password",
                    "Password for the trust store file, required when SSL is true",
                    ConfigurationField.Optional.OPTIONAL));

            cr.addField(new TextField(
                    CK_SSL_ENABLED_PROTOCOL,
                    "ssl protocol",
                    "TLSv1.2",
                    "Enabled Protocols for SSL, required when SSL is true",
                    ConfigurationField.Optional.OPTIONAL));



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
                    "What to do when there is no initial offset in ZooKeeper or if an offset is out of range",
                    ConfigurationField.Optional.OPTIONAL));

            return cr;
        }
    }
}
