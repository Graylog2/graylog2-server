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
package org.graylog.integrations.aws.transports;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.graylog.aws.AWSAsyncProxyConfigurationProvider;
import org.graylog.aws.AWSProxyConfigurationProvider;
import org.graylog.integrations.aws.AWSAuthFactory;
import org.graylog.integrations.aws.AWSClientBuilderUtil;
import org.graylog.integrations.aws.AWSMessageType;
import org.graylog.integrations.aws.resources.requests.AWSRequest;
import org.graylog.integrations.aws.resources.requests.AWSRequestImpl;
import org.graylog2.Configuration;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.kinesis.coordinator.CoordinatorConfig;
import software.amazon.kinesis.leases.LeaseManagementConfig;
import software.amazon.kinesis.metrics.MetricsConfig;
import software.amazon.kinesis.metrics.MetricsLevel;
import software.amazon.kinesis.retrieval.RetrievalConfig;
import software.amazon.kinesis.retrieval.polling.PollingConfig;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Boots the KCL-based Kinesis consumer against a local AWS emulator and verifies a record
 * round-trip. Also proves the consumer starts without the (excluded) Glue schema-registry-serde
 * dependency on the classpath and guards future amazon-kinesis-client upgrades.
 */
class KinesisConsumerIT {

    private static final String STREAM = "kinesis-consumer-it-stream";
    private static final String TEST_MESSAGE = "kinesis-consumer-it test message";
    private static final String PASSWORD_SECRET = "0123456789abcdef";
    // KCL derives the lease table name from the application name; sourcing it from production code
    // makes it impossible for the pre-seeded table (below) to drift out of sync with the consumer.
    private static final String LEASE_TABLE = KinesisConsumer.applicationName(STREAM);

    private static final KinesisEmulatorContainer EMULATOR = new KinesisEmulatorContainer();
    private static final String KCL_LOGGER = "software.amazon.kinesis";

    private static Level previousKclLogLevel;

    @BeforeAll
    static void setUp() {
        // KCL logs its lease lifecycle milestones (lease taking, shard consumer creation, worker
        // state) at INFO; without them a hanging consumer is nearly undiagnosable from the test
        // output. Bump to DEBUG locally when investigating — but beware, DEBUG is extremely chatty
        // (sub-second renewer/prefetcher/metrics loops). Restored in tearDown() because failsafe
        // runs all ITs in one JVM fork.
        previousKclLogLevel = LogManager.getLogger(KCL_LOGGER).getLevel();
        Configurator.setLevel(KCL_LOGGER, Level.INFO);

        EMULATOR.start();
        final String shardId;
        try (KinesisClient kinesis = KinesisClient.builder()
                .endpointOverride(EMULATOR.endpoint())
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build()) {
            kinesis.createStream(r -> r.streamName(STREAM).shardCount(1));
            kinesis.waiter().waitUntilStreamExists(r -> r.streamName(STREAM));
            // Put the record before the consumer starts. The consumer reads from TRIM_HORIZON
            // (via the pre-seeded lease checkpoint below), so it sees the record.
            kinesis.putRecord(r -> r.streamName(STREAM)
                    .partitionKey("test-partition-key")
                    .data(SdkBytes.fromUtf8String(TEST_MESSAGE)));
            shardId = kinesis.listShards(r -> r.streamName(STREAM)).shards().get(0).shardId();
        }

        // Pre-seed the lease table with an unowned lease for the single shard. Without this, KCL's
        // Scheduler.shouldInitiateLeaseSync() waits a hardcoded random 1-30s jitter (thundering-herd
        // protection for real fleets) before the initial shard sync creates the lease. With the
        // lease present, initialization proceeds immediately and the lease taker can acquire it on
        // its first pass. The attributes are the lenient minimal set read by KCL's
        // DynamoDBLeaseSerializer; no leaseOwner attribute means "available". KCL adds its
        // LeaseOwnerToLeaseKeyIndex GSI to the existing table by itself.
        // Deliberately no fallback: if a future KCL stops honoring the pre-seeded lease, its own
        // shard sync would create leases at the default LATEST position, the pre-put record would
        // never arrive, and the test FAILS by timeout — loud, instead of silently passing slower.
        try (DynamoDbClient dynamo = DynamoDbClient.builder()
                .endpointOverride(EMULATOR.endpoint())
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build()) {
            dynamo.createTable(r -> r.tableName(LEASE_TABLE)
                    .attributeDefinitions(a -> a.attributeName("leaseKey").attributeType(ScalarAttributeType.S))
                    .keySchema(k -> k.attributeName("leaseKey").keyType(KeyType.HASH))
                    .billingMode(BillingMode.PAY_PER_REQUEST));
            dynamo.waiter().waitUntilTableExists(r -> r.tableName(LEASE_TABLE));
            dynamo.putItem(r -> r.tableName(LEASE_TABLE).item(Map.of(
                    "leaseKey", AttributeValue.fromS(shardId),
                    "leaseCounter", AttributeValue.fromN("0"),
                    "checkpoint", AttributeValue.fromS("TRIM_HORIZON"),
                    "checkpointSubSequenceNumber", AttributeValue.fromN("0"),
                    "ownerSwitchesSinceCheckpoint", AttributeValue.fromN("0"))));
        }
    }

    @AfterAll
    static void tearDown() {
        Configurator.setLevel(KCL_LOGGER, previousKclLogLevel);
        EMULATOR.stop();
    }

    @Test
    void consumesRecordFromKinesisStream() throws Exception {
        final BlockingQueue<RawMessage> received = new LinkedBlockingQueue<>();
        // Captures the first failure signal, whether the consumer thread dies with an uncaught
        // exception or KCL reports a failure through the InputFailureRecorder. Lets the test
        // abort immediately with the root cause instead of blocking for the full timeout.
        final AtomicReference<Throwable> consumerFailure = new AtomicReference<>();

        final EncryptedValueService encryptedValueService = new EncryptedValueService(PASSWORD_SECRET);
        final String endpoint = EMULATOR.endpoint().toString();
        final AWSRequest request = AWSRequestImpl.builder()
                .region(Region.US_EAST_1.id())
                .awsAccessKeyId("test")
                .awsSecretAccessKey(encryptedValueService.encrypt("test"))
                .kinesisEndpoint(endpoint)
                .dynamodbEndpoint(endpoint)
                .cloudwatchEndpoint(endpoint)
                .build();

        final AWSClientBuilderUtil clientBuilderUtil = new AWSClientBuilderUtil(
                AWSAuthFactory::new,
                encryptedValueService,
                new Configuration(),
                new AWSProxyConfigurationProvider(null),
                new AWSAsyncProxyConfigurationProvider(null));

        final KinesisTransport transport = mock(KinesisTransport.class);
        when(transport.isThrottled()).thenReturn(false);

        // The consumer reports KCL failures through the InputFailureRecorder instead of throwing, so
        // capture those signals as well. Transient vs. terminal matters here: the 2-arg setFailing()
        // comes from the TaskExecutionListener on any failed KCL task, which KCL retries — record it
        // only as diagnostic context. The 3-arg setFailing() comes from
        // onAllInitializationAttemptsFailed, which is terminal — abort on it.
        final AtomicReference<String> lastTaskFailure = new AtomicReference<>();
        final InputFailureRecorder failureRecorder = mock(InputFailureRecorder.class);
        doAnswer(invocation -> {
            lastTaskFailure.set(invocation.getArgument(1, String.class));
            return null;
        }).when(failureRecorder).setFailing(any(), anyString());
        doAnswer(invocation -> {
            final Throwable cause = invocation.getArgument(2, Throwable.class);
            consumerFailure.compareAndSet(null,
                    cause != null ? cause : new RuntimeException(invocation.getArgument(1, String.class)));
            return null;
        }).when(failureRecorder).setFailing(any(), anyString(), any());

        // Anonymous subclass tunes KCL's coordination timings, which default to production
        // values (10s failover, 10s shard polling, LATEST initial position) that would make
        // this test take over a minute.
        final KinesisConsumer consumer = new KinesisConsumer(
                new SimpleNodeId("00000000-0000-0000-0000-000000000000"),
                transport,
                new ObjectMapperProvider().get(),
                received::add,
                STREAM,
                AWSMessageType.KINESIS_RAW,
                10_000,
                request,
                clientBuilderUtil,
                failureRecorder) {
            @Override
            void customizeSchedulerConfigs(CoordinatorConfig coordinatorConfig,
                                           LeaseManagementConfig leaseManagementConfig,
                                           MetricsConfig metricsConfig,
                                           RetrievalConfig retrievalConfig,
                                           PollingConfig pollingConfig) {
                coordinatorConfig.parentShardPollIntervalMillis(1000);
                // The ShardConsumer state machine advances one state per worker-loop tick
                // (create -> initialize -> process), so the default 1s interval costs ~3s
                // before the first record is delivered.
                coordinatorConfig.shardConsumerDispatchPollIntervalMillis(100);
                leaseManagementConfig.failoverTimeMillis(500);
                metricsConfig.metricsLevel(MetricsLevel.NONE);
                // KCL 3.x assigns leases through a leader-driven LeaseAssignmentManager built around
                // worker-utilization metrics and EC2/ECS/EKS platform detection. Neither exists on a
                // dev machine or a local emulator: the IMDS probe burns ~15s in connect timeouts at
                // Scheduler construction, and leases never get assigned, so no GetRecords call is ever
                // made and the consumer hangs. Run the official 2.x-compatible assignment path
                // (workers take unowned leases directly) and skip the platform probe
                // (disableWorkerMetrics has no effect beyond skipping the probe).
                coordinatorConfig.clientVersionConfig(
                        CoordinatorConfig.ClientVersionConfig.CLIENT_VERSION_CONFIG_COMPATIBLE_WITH_2X);
                leaseManagementConfig.workerUtilizationAwareAssignmentConfig().disableWorkerMetrics(true);
                // Graceful lease handoff waits a default 30s for the previous owner to checkpoint and
                // release. There are no previous owners in this test — don't wait for them.
                leaseManagementConfig.gracefulLeaseHandoffConfig().isGracefulLeaseHandoffEnabled(false);
                pollingConfig.idleTimeBetweenReadsInMillis(200);
            }
        };

        final Thread consumerThread = new Thread(consumer, "kinesis-consumer-it");
        consumerThread.setDaemon(true);
        consumerThread.setUncaughtExceptionHandler((t, e) -> consumerFailure.compareAndSet(null, e));
        consumerThread.start();
        try {
            // Poll in short intervals up to the deadline so the test fails fast — with the root
            // cause — when the consumer dies instead of blocking for the full timeout. The healthy
            // path delivers in well under 10s; the deadline is headroom for slow CI runners.
            final long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
            RawMessage message = null;
            while (message == null && System.nanoTime() < deadlineNanos) {
                message = received.poll(1, TimeUnit.SECONDS);
                if (message == null && (consumerFailure.get() != null || !consumerThread.isAlive())) {
                    fail("Kinesis consumer died before delivering a record", consumerFailure.get());
                }
            }
            // A terminal failure may have landed in the final poll window after the deadline check.
            if (message == null && consumerFailure.get() != null) {
                fail("Kinesis consumer died before delivering a record", consumerFailure.get());
            }
            final String taskFailure = lastTaskFailure.get();
            assertThat(message)
                    .as("KCL consumer should deliver the pre-put record within the timeout"
                            + (taskFailure == null ? "" : "; last transient KCL task failure: " + taskFailure))
                    .isNotNull();
            assertThat(new String(message.getPayload(), StandardCharsets.UTF_8)).contains(TEST_MESSAGE);
        } finally {
            // Expect a few benign "LeaderDecider uninitialized" ERROR logs here: KCL's graceful
            // shutdown tears down its migration components before the worker loop exits, and each
            // loop tick in that window throws, gets caught and logged by KCL, and is retried. The
            // 100ms dispatch interval makes this pre-existing KCL race visible; shutdown still
            // completes cleanly (verified by the bounded join below).
            consumer.stop();
            consumerThread.join(TimeUnit.SECONDS.toMillis(30));
        }
    }
}
