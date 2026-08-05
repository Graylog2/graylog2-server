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


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.graylog.integrations.aws.AWSAuthorizationFailureDetector;
import org.graylog.integrations.aws.AWSClientBuilderUtil;
import org.graylog.integrations.aws.AWSMessageType;
import org.graylog.integrations.aws.resources.requests.AWSRequest;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClientBuilder;
import software.amazon.kinesis.checkpoint.CheckpointConfig;
import software.amazon.kinesis.common.ConfigsBuilder;
import software.amazon.kinesis.coordinator.CoordinatorConfig;
import software.amazon.kinesis.coordinator.NoOpWorkerStateChangeListener;
import software.amazon.kinesis.coordinator.Scheduler;
import software.amazon.kinesis.coordinator.WorkerStateChangeListener;
import software.amazon.kinesis.leases.LeaseManagementConfig;
import software.amazon.kinesis.lifecycle.LifecycleConfig;
import software.amazon.kinesis.lifecycle.NoOpTaskExecutionListener;
import software.amazon.kinesis.lifecycle.TaskExecutionListener;
import software.amazon.kinesis.lifecycle.TaskOutcome;
import software.amazon.kinesis.lifecycle.TaskType;
import software.amazon.kinesis.lifecycle.events.TaskExecutionListenerInput;
import software.amazon.kinesis.metrics.MetricsConfig;
import software.amazon.kinesis.processor.ProcessorConfig;
import software.amazon.kinesis.retrieval.RetrievalConfig;
import software.amazon.kinesis.retrieval.polling.PollingConfig;

import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * A runnable task that starts the Kinesis Consumer.
 * Utilizes the {@see <a href="https://github.com/awslabs/amazon-kinesis-client">Kinesis Client Library</a>}.
 */
public class KinesisConsumer implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(KinesisConsumer.class);
    private static final int GRACEFUL_SHUTDOWN_TIMEOUT = 20;
    private static final TimeUnit GRACEFUL_SHUTDOWN_TIMEOUT_UNIT = TimeUnit.SECONDS;

    private final String kinesisStreamName;
    private final NodeId nodeId;
    private final KinesisTransport transport;
    private final Integer recordBatchSize;
    private final ObjectMapper objectMapper;
    private final AWSMessageType awsMessageType;
    private final Consumer<RawMessage> handleMessageCallback;
    private final AWSRequest request;
    private final AWSClientBuilderUtil awsClientBuilderUtil;
    private final InputFailureRecorder inputFailureRecorder;
    private final boolean migrateToSingleTable;
    private final AtomicBoolean authorizationFailureHandled = new AtomicBoolean();
    private Scheduler kinesisScheduler;

    KinesisConsumer(NodeId nodeId,
                    KinesisTransport transport,
                    ObjectMapper objectMapper,
                    Consumer<RawMessage> handleMessageCallback,
                    String kinesisStreamName,
                    AWSMessageType awsMessageType,
                    int recordBatchSize, AWSRequest request,
                    AWSClientBuilderUtil awsClientBuilderUtil,
                    InputFailureRecorder inputFailureRecorder,
                    boolean migrateToSingleTable) {
        Preconditions.checkArgument(StringUtils.isNotBlank(kinesisStreamName), "A Kinesis stream name is required.");
        Preconditions.checkNotNull(awsMessageType, "A AWSMessageType is required.");

        this.nodeId = requireNonNull(nodeId, "nodeId");
        this.transport = transport;
        this.handleMessageCallback = handleMessageCallback;
        this.kinesisStreamName = requireNonNull(kinesisStreamName, "kinesisStream");
        this.objectMapper = objectMapper;
        this.awsMessageType = awsMessageType;
        this.recordBatchSize = recordBatchSize;
        this.request = request;
        this.awsClientBuilderUtil = awsClientBuilderUtil;
        this.inputFailureRecorder = inputFailureRecorder;
        this.migrateToSingleTable = migrateToSingleTable;
    }

    @Override
    public void run() {

        LOG.debug("Starting the Kinesis Consumer.");
        AwsCredentialsProvider credentialsProvider = awsClientBuilderUtil.createCredentialsProviderWithStsProxy(request);

        final Region region = Region.of(request.region());

        // One detector per client: a success against one service must not reset the denial count of another,
        // or a healthy Kinesis poll would mask a permanent DynamoDB denial forever. CloudWatch is deliberately
        // left out: losing metrics is not a reason to fail an input that is otherwise ingesting fine.
        final AWSAuthorizationFailureDetector dynamoDbAuthFailures =
                new AWSAuthorizationFailureDetector(this::handleAuthorizationFailure);
        final AWSAuthorizationFailureDetector kinesisAuthFailures =
                new AWSAuthorizationFailureDetector(this::handleAuthorizationFailure);

        final DynamoDbAsyncClientBuilder dynamoDbClientBuilder = DynamoDbAsyncClient.builder();
        awsClientBuilderUtil.initializeBuilder(dynamoDbClientBuilder, request.dynamodbEndpoint(), region, credentialsProvider);
        dynamoDbClientBuilder.httpClientBuilder(awsClientBuilderUtil.asyncHttpClientBuilder());
        dynamoDbClientBuilder.overrideConfiguration(c -> c.addExecutionInterceptor(dynamoDbAuthFailures));
        final CloudWatchAsyncClientBuilder cloudwatchClientBuilder = CloudWatchAsyncClient.builder();
        awsClientBuilderUtil.initializeBuilder(cloudwatchClientBuilder, request.cloudwatchEndpoint(), region, credentialsProvider);
        cloudwatchClientBuilder.httpClientBuilder(awsClientBuilderUtil.asyncHttpClientBuilder());

        // The Kinesis Client Library normally configures the async client through
        // KinesisClientUtil.createKinesisAsyncClient(), but that installs its own HTTP client builder and would discard
        // our proxy configuration. We therefore apply the proxy-aware, HTTP/2-enabled builder ourselves.
        final KinesisAsyncClientBuilder kinesisAsyncClientBuilder = KinesisAsyncClient.builder();
        awsClientBuilderUtil.initializeBuilder(kinesisAsyncClientBuilder, request.kinesisEndpoint(), region, credentialsProvider);
        kinesisAsyncClientBuilder.httpClientBuilder(awsClientBuilderUtil.kinesisAsyncHttpClientBuilder());
        kinesisAsyncClientBuilder.overrideConfiguration(c -> c.addExecutionInterceptor(kinesisAuthFailures));

        // All three clients are kept open for the lifetime of the KCL Scheduler (kinesisScheduler.run() blocks).
        // Try-with-resources ensures the Netty event loop groups and connection pools are released when the scheduler
        // exits, whether normally or due to an exception.
        try (DynamoDbAsyncClient dynamoClient = dynamoDbClientBuilder.build();
             CloudWatchAsyncClient cloudWatchClient = cloudwatchClientBuilder.build();
             KinesisAsyncClient kinesisAsyncClient = kinesisAsyncClientBuilder.build()) {

            final String workerId = String.format(Locale.ENGLISH, "graylog-node-%s", nodeId.anonymize());
            LOG.debug("Using workerId [{}].", workerId);

            // The application name needs to be unique per input/consumer.
            final String applicationName = applicationName(kinesisStreamName);
            LOG.debug("Using Kinesis applicationName [{}].", applicationName);

            // The KinesisShardProcessorFactory contains the message processing logic.
            final KinesisShardProcessorFactory kinesisShardProcessorFactory = new KinesisShardProcessorFactory(objectMapper, transport, handleMessageCallback, kinesisStreamName, awsMessageType);

            ConfigsBuilder configsBuilder = new ConfigsBuilder(kinesisStreamName, applicationName,
                    kinesisAsyncClient, dynamoClient, cloudWatchClient,
                    workerId,
                    kinesisShardProcessorFactory);

            final PollingConfig pollingConfig = new PollingConfig(kinesisStreamName, kinesisAsyncClient);

            // Default max records per request is 10k.
            if (recordBatchSize != null) {
                LOG.debug("Using explicit batch size [{}]", recordBatchSize);
                pollingConfig.maxRecords(recordBatchSize);
            }
            WorkerStateChangeListener workerStateChangeListener = new NoOpWorkerStateChangeListener() {
                @Override
                public void onAllInitializationAttemptsFailed(Throwable e) {
                    inputFailureRecorder.setFailing(
                            KinesisConsumer.class,
                            String.format(Locale.ROOT, "Initialization for Kinesis stream <%s> failed.", kinesisStreamName), e);
                }
            };

            TaskExecutionListener taskExecutionListener = new NoOpTaskExecutionListener() {
                @Override
                public void afterTaskExecution(TaskExecutionListenerInput input) {
                    if (TaskOutcome.FAILURE.equals(input.taskOutcome())) {
                        inputFailureRecorder.setFailing(KinesisConsumer.class,
                                String.format(Locale.ROOT, "Errors for Kinesis stream <%s>!", kinesisStreamName));
                    } else if (TaskOutcome.SUCCESSFUL.equals(input.taskOutcome()) && TaskType.PROCESS.equals(input.taskType())) {
                        recordTaskSuccess();
                    }
                }
            };

            // ConfigsBuilder accessors create a new config object on every call, so each config must be
            // materialized exactly once and the same instances passed to the Scheduler — otherwise the
            // customizeSchedulerConfigs() customizations would be silently discarded.
            final CheckpointConfig checkpointConfig = configsBuilder.checkpointConfig();
            final CoordinatorConfig coordinatorConfig = configsBuilder.coordinatorConfig()
                    .workerStateChangeListener(workerStateChangeListener);
            if (migrateToSingleTable) {
                LOG.info("Enabling one-time KCL metadata migration to the lease table.");
                coordinatorConfig.migrateAllEntitiesToLeaseTable(true);
            }
            final LeaseManagementConfig leaseManagementConfig = configsBuilder.leaseManagementConfig();
            final LifecycleConfig lifecycleConfig = configsBuilder.lifecycleConfig()
                    .taskExecutionListener(taskExecutionListener);
            final MetricsConfig metricsConfig = configsBuilder.metricsConfig();
            final ProcessorConfig processorConfig = configsBuilder.processorConfig();
            final RetrievalConfig retrievalConfig = configsBuilder.retrievalConfig()
                    .retrievalSpecificConfig(pollingConfig);

            customizeSchedulerConfigs(coordinatorConfig, leaseManagementConfig, metricsConfig, retrievalConfig, pollingConfig);

            this.kinesisScheduler = new Scheduler(checkpointConfig, coordinatorConfig, leaseManagementConfig,
                    lifecycleConfig, metricsConfig, processorConfig, retrievalConfig);

            LOG.debug("Starting Kinesis scheduler.");
            kinesisScheduler.run();
            LOG.debug("After Kinesis scheduler stopped.");
        }
    }

    /**
     * Hook that allows tests to tune KCL coordination timings (e.g. lease failover, polling intervals)
     * before the {@link Scheduler} is built. KCL's defaults are appropriate for production but make
     * integration tests needlessly slow. Production code must not override this.
     */
    @VisibleForTesting
    void customizeSchedulerConfigs(CoordinatorConfig coordinatorConfig,
                                   LeaseManagementConfig leaseManagementConfig,
                                   MetricsConfig metricsConfig,
                                   RetrievalConfig retrievalConfig,
                                   PollingConfig pollingConfig) {
        // Intentionally empty: production uses KCL defaults.
    }

    /**
     * The KCL application name used for a stream. KCL derives the DynamoDB lease table name from it,
     * which integration tests rely on when pre-seeding leases.
     */
    @VisibleForTesting
    static String applicationName(String kinesisStreamName) {
        return String.format(Locale.ENGLISH, "graylog-aws-plugin-%s", kinesisStreamName);
    }

    /**
     * KCL keeps draining already-owned leases while it shuts down, and each successful PROCESS task would
     * otherwise report the input healthy again. A terminal authorization failure must survive that, or the input
     * ends up displaying RUNNING with no consumer behind it.
     */
    @VisibleForTesting
    void recordTaskSuccess() {
        if (authorizationFailureHandled.get()) {
            return;
        }
        inputFailureRecorder.setRunning();
    }

    /**
     * Fails the input and stops the KCL scheduler after an AWS authorization denial that retrying cannot fix.
     * Without this, KCL retries such calls on a fixed schedule for as long as the input is running, logging an
     * ERROR with a stack trace every time while consuming no records.
     */
    @VisibleForTesting
    void handleAuthorizationFailure(Throwable cause) {
        if (!authorizationFailureHandled.compareAndSet(false, true)) {
            return;
        }
        inputFailureRecorder.setFailing(KinesisConsumer.class, String.format(Locale.ROOT,
                "AWS authorization failure for Kinesis stream <%s>. The input was stopped because retrying "
                        + "cannot resolve a missing permission. Grant it, then start the input again.",
                kinesisStreamName), cause);

        // This runs on an AWS SDK event loop thread, and stop() blocks while KCL shuts down using that same
        // event loop, so it must not be called inline.
        final Thread shutdownThread = new Thread(() -> {
            try {
                stop();
            } catch (Exception e) {
                LOG.error("Failed to stop the Kinesis consumer for stream <{}> after an AWS authorization failure.",
                        kinesisStreamName, e);
            }
        }, "aws-kinesis-auth-failure-shutdown");
        shutdownThread.setDaemon(true);
        shutdownThread.start();
    }

    /**
     * Stops the KinesisConsumer. Finishes processing the current batch of data already received from Kinesis
     * before shutting down.
     */
    public void stop() {
        if (kinesisScheduler != null) {
            Future<Boolean> gracefulShutdownFuture = kinesisScheduler.startGracefulShutdown();
            LOG.info("Waiting up to 20 seconds for Kinesis Consumer shutdown to complete.");
            try {
                gracefulShutdownFuture.get(GRACEFUL_SHUTDOWN_TIMEOUT, GRACEFUL_SHUTDOWN_TIMEOUT_UNIT);
            } catch (InterruptedException e) {
                LOG.info("Interrupted while waiting for graceful shutdown. Continuing.");
            } catch (ExecutionException e) {
                LOG.error("Exception while executing graceful shutdown.", e);
            } catch (TimeoutException e) {
                LOG.error("Timeout while waiting for shutdown.  Scheduler may not have exited.");
                kinesisScheduler.shutdown();
            }
        }
    }
}
