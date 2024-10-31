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
package org.graylog.testing.kafka;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;
import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * An Apache Kafka container that is optimized for fast startup. The container is using the
 * <a href="https://hub.docker.com/r/bitnami/kafka">bitnami/kafka</a> image.
 */
public class KafkaContainer extends GenericContainer<KafkaContainer> {
    public enum Version {
        V34("3.4.0");

        private final String version;

        Version(String version) {
            this.version = version;
        }

        public String getVersion() {
            return version;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(KafkaContainer.class);
    private static final Version DEFAULT_VERSION = Version.V34;
    private static final String KAFKA_ADVERTISED_LISTENERS_FILE = "/.env-kafka-advertised-listeners";

    private Admin adminClient = null;

    private final Network network;

    /**
     * Returns a new instance using the default Kafka version.
     *
     * @return new instance
     */
    public static KafkaContainer create() {
        return new KafkaContainer(DEFAULT_VERSION, Network.newNetwork());
    }

    /**
     * Returns a new instance using the give Kafka version.
     *
     * @return new instance
     */
    public static KafkaContainer create(Version version) {
        return new KafkaContainer(version, Network.newNetwork());
    }

    @SuppressWarnings("resource")
    private KafkaContainer(Version version, Network network) {
        super(DockerImageName.parse(f("bitnami/kafka:%s", version.getVersion())));
        this.network = network;

        withExposedPorts(9092);
        withNetwork(requireNonNull(network, "network cannot be null"));

        withEnv("KAFKA_HEAP_OPTS", "-Xmx256m -Xms256m");
        withEnv("ALLOW_PLAINTEXT_LISTENER", "yes");
        withEnv("KAFKA_KRAFT_CLUSTER_ID", generateKraftClusterId());
        withEnv("KAFKA_CFG_NODE_ID", "1");
        withEnv("KAFKA_CFG_CONTROLLER_QUORUM_VOTERS", "1@127.0.0.1:9093");
        withEnv("KAFKA_CFG_PROCESS_ROLES", "broker,controller");
        withEnv("KAFKA_CFG_CONTROLLER_LISTENER_NAMES", "CONTROLLER");
        withEnv("KAFKA_CFG_LISTENERS", "EXTERNAL://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093,INTERNAL://0.0.0.0:9094");
        withEnv("KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP", "EXTERNAL:PLAINTEXT,CONTROLLER:PLAINTEXT,INTERNAL:PLAINTEXT");
        withEnv("KAFKA_CFG_INTER_BROKER_LISTENER_NAME", "INTERNAL");

        // The following settings speed up group rebalancing. Without it, it can take several seconds before a
        // consumer receives the first message.
        withEnv("KAFKA_CFG_GROUP_INITIAL_REBALANCE_DELAY_MS", "0");
        withEnv("KAFKA_CFG_OFFSETS_TOPIC_NUM_PARTITIONS", "1");
        withEnv("KAFKA_CFG_OFFSETS_TOPIC_REPLICATION_FACTOR", "1");
        withEnv("KAFKA_CFG_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1");
        withEnv("KAFKA_CFG_TRANSACTION_STATE_LOG_MIN_ISR", "1");

        // Override the default entrypoint, so we only start Kafka once our listeners files is written.
        withCreateContainerCmdModifier(cmd -> cmd.withEntrypoint("sh"));
        withCommand("-c",
                // The EXTERNAL listener must use the mapped host port for port 9092. We only know this once the
                // container is started, so we have to set the advertised.listeners in the command script.
                f("while [ ! -f %s ]; do sleep 0.1; done; export KAFKA_CFG_ADVERTISED_LISTENERS=\"$(cat %s)\"; /entrypoint.sh /run.sh",
                        KAFKA_ADVERTISED_LISTENERS_FILE, KAFKA_ADVERTISED_LISTENERS_FILE)
        );

        waitingFor(Wait.forLogMessage(".*Kafka Server started.*", 1).withStartupTimeout(Duration.ofSeconds(60)));
    }

    private static String generateKraftClusterId() {
        // The cluster id is a unique and immutable identifier assigned to a Kafka cluster. The cluster id can
        // have a maximum of 22 characters and the allowed characters are defined by the regular expression
        // [a-zA-Z0-9_\-]+, which corresponds to the characters used by the URL-safe Base64 variant with no padding.
        // Source: https://kafka.apache.org/documentation/#impl_clusterid
        return Base64.getEncoder()
                .encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8))
                .substring(0, 22);
    }

    /**
     * Returns the mapped port number that clients outside the Docker network can use to connect to Kafka.
     *
     * @return the mapped port number
     */
    public int getKafkaPort() {
        return getMappedPort(9092);
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        super.containerIsStarting(containerInfo);

        final String hostName = containerInfo.getConfig().getHostName();
        // Advertise the correct host and port to the external client.
        final String value = f("EXTERNAL://%s:%d,INTERNAL://%s:9094", getHost(), getKafkaPort(), hostName);

        copyFileToContainer(
                Transferable.of(value),
                KAFKA_ADVERTISED_LISTENERS_FILE
        );
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        super.containerIsStarted(containerInfo);

        this.adminClient = Admin.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:" + getKafkaPort()
        ));
    }

    /**
     * Returns a new {@code KafkaProducer<String, byte[]>} instance that is connected to the Kafka container.
     *
     * @return the new producer instance
     */
    public KafkaProducer<String, byte[]> createByteArrayProducer() {
        final var props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:" + getKafkaPort());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "graylog-node-" + UUID.randomUUID());
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.LINGER_MS_CONFIG, 0);

        return new KafkaProducer<>(props);
    }

    /**
     * Creates a new Kafka topic with default settings.
     *
     * @param topicName the name of the new topic
     * @throws Exception when topic creation fails
     */
    @SuppressWarnings("resource")
    public void createTopic(String topicName) throws Exception {
        LOG.info("Creating topic: {}", topicName);
        adminClient().createTopics(Set.of(new NewTopic(topicName, 1, (short) 1)))
                .all()
                .get(30, TimeUnit.SECONDS);
    }

    /**
     * Returns a set of existing topics.
     *
     * @return set of topic strings
     * @throws Exception when topic retrieval fails
     */
    @SuppressWarnings("resource")
    public Set<String> listTopics() throws Exception {
        return adminClient().listTopics().names().get(30, TimeUnit.SECONDS);
    }

    /**
     * Returns a Kafka Admin client instance. Can only be used once the container is fully started.
     *
     * @return the admin client instance
     */
    public Admin adminClient() {
        return requireNonNull(adminClient, "adminClient is not initialized yet");
    }

    @Override
    public void close() {
        super.close();
        network.close();
    }
}
