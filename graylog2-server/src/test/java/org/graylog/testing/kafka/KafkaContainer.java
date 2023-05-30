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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public class KafkaContainer extends GenericContainer<KafkaContainer> {
    public enum Version {
        V34("3.4");


        private final String version;
        Version(String version) {
            this.version = version;
        }

        public String getVersion() {
            return version;
        }

    }

    private static final Logger LOG = LoggerFactory.getLogger(KafkaContainer.class);
    private static final String KAFKA_ADVERTISED_LISTENERS_FILE = "/.env-kafka-advertised-listeners";

    private Admin adminClient = null;

    public KafkaContainer() {
        this(Version.V34, Network.newNetwork());
    }

    @SuppressWarnings("resource")
    public KafkaContainer(Version version, Network network) {
        super(DockerImageName.parse("bitnami/kafka:%s".formatted(version.getVersion())));

        withExposedPorts(9092);
        withNetwork(requireNonNull(network, "network cannot be null"));

        withEnv("ALLOW_PLAINTEXT_LISTENER", "yes");
        withEnv("KAFKA_KRAFT_CLUSTER_ID", "abcdefghijklmnopqrstuv");
        withEnv("KAFKA_CFG_NODE_ID", "1");
        withEnv("KAFKA_CFG_CONTROLLER_QUORUM_VOTERS", "1@127.0.0.1:9093");
        withEnv("KAFKA_CFG_PROCESS_ROLES", "broker,controller");
        withEnv("KAFKA_CFG_CONTROLLER_LISTENER_NAMES", "CONTROLLER");
        withEnv("KAFKA_CFG_LISTENERS", "EXTERNAL://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093,INTERNAL://0.0.0.0:9094");
        withEnv("KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP", "EXTERNAL:PLAINTEXT,CONTROLLER:PLAINTEXT,INTERNAL:PLAINTEXT");
        // The bitnami image uses the content of _FILE variables as file name to read the value from.
        withEnv("KAFKA_CFG_ADVERTISED_LISTENERS_FILE", KAFKA_ADVERTISED_LISTENERS_FILE);
        withEnv("KAFKA_CFG_INTER_BROKER_LISTENER_NAME", "INTERNAL");

        // The following settings speed up group rebalancing. Without it, it can take several seconds before a
        // consumer receives the first message.
        withEnv("KAFKA_CFG_GROUP_INITIAL_REBALANCE_DELAY_MS", "0");
        withEnv("KAFKA_CFG_OFFSETS_TOPIC_NUM_PARTITIONS", "1");
        withEnv("KAFKA_CFG_OFFSETS_TOPIC_REPLICATION_FACTOR", "1");
        withEnv("KAFKA_CFG_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1");
        withEnv("KAFKA_CFG_TRANSACTION_STATE_LOG_MIN_ISR", "1");

        // Override the default entrypoint so we only start Kafka once our listeners files is written.
        withCreateContainerCmdModifier(cmd -> cmd.withEntrypoint("sh"));
        withCommand("-c", "while [ ! -f " + KAFKA_ADVERTISED_LISTENERS_FILE + " ]; do sleep 0.1; done; /entrypoint.sh /run.sh");

        waitingFor(Wait.forLogMessage(".*Kafka Server started.*", 1).withStartupTimeout(Duration.ofSeconds(5)));
    }

    public int getKafkaPort() {
        return getMappedPort(9092);
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        super.containerIsStarting(containerInfo);

        final String hostName = containerInfo.getConfig().getHostName();
        // Advertise the correct host and port to the external client.
        final String value = "EXTERNAL://%s:%d,INTERNAL://%s:9094".formatted(getHost(), getKafkaPort(), hostName);

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

    public KafkaProducer<String, byte[]> createByteArrayProducer() {
        final var props = new Properties();
        props.put("bootstrap.servers", "localhost:" + getKafkaPort());
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        props.put("client.id", "graylog-node-" + UUID.randomUUID());
        props.put("acks", "1");
        props.put("linger.ms", 0);

        return new KafkaProducer<>(props);
    }

    public void createTopic(String topicName) throws Exception {
        LOG.info("Creating topic: {}", topicName);
        adminClient.createTopics(Set.of(new NewTopic(topicName, 1, (short) 1)))
                .all()
                .get(30, TimeUnit.SECONDS);
    }

    public Set<String> listTopics() throws Exception {
        return adminClient.listTopics().names().get(30, TimeUnit.SECONDS);
    }
}
