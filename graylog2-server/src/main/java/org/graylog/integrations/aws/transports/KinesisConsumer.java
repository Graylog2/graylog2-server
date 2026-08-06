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
import java.util.function.LongSupplier;

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

    /**
     * Monotonic clock handed to the authorization-failure detectors, so a test can drive their threshold.
     */
    private final LongSupplier nanoClock;
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
        this(nodeId, transport, objectMapper, handleMessageCallback, kinesisStreamName, awsMessageType,
                recordBatchSize, request, awsClientBuilderUtil, inputFailureRecorder, migrateToSingleTable,
                System::nanoTime);
    }

    @VisibleForTesting
    KinesisConsumer(NodeId nodeId,
                    KinesisTransport transport,
                    ObjectMapper objectMapper,
                    Consumer<RawMessage> handleMessageCallback,
                    String kinesisStreamName,
                    AWSMessageType awsMessageType,
                    int recordBatchSize, AWSRequest request,
                    AWSClientBuilderUtil awsClientBuilderUtil,
                    InputFailureRecorder inputFailureRecorder,
                    boolean migrateToSingleTable,
                    LongSupplier nanoClock) {
        Preconditions.checkArgument(StringUtils.isNotBlank(kinesisStreamName), "A Kinesis stream name is required.");
        Preconditions.checkNotNull(awsMessageType, "A AWSMessageType is required.");

        this.nanoClock = requireNonNull(nanoClock, "nanoClock");
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
        final AwsCredentialsProvider credentialsProvider = awsClientBuilderUtil.createCredentialsProviderWithStsProxy(request);

        final ClientBuilders clientBuilders = createClientBuilders(Region.of(request.region()), credentialsProvider);

        // All three clients are kept open for the lifetime of the KCL Scheduler (kinesisScheduler.run() blocks).
        // Try-with-resources ensures the Netty event loop groups and connection pools are released when the scheduler
        // exits, whether normally or due to an exception.
        try (DynamoDbAsyncClient dynamoClient = clientBuilders.dynamoDb().build();
             CloudWatchAsyncClient cloudWatchClient = clientBuilders.cloudWatch().build();
             KinesisAsyncClient kinesisAsyncClient = clientBuilders.kinesis().build()) {

            final String workerId = String.format(Locale.ENGLISH, "graylog-node-%s", nodeId.anonymize());
            LOG.debug("Using workerId [{}].", workerId);

            // The application name is per stream, so two inputs on the same stream share a KCL lease table.
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
     * The three async client builders KCL needs, with an authorization-failure detector attached to the two clients
     * whose denials are fatal. Extracted from {@link #run()} so a test can assert that wiring: which clients are
     * watched, that each gets its own detector, and that CloudWatch gets none.
     */
    @VisibleForTesting
    ClientBuilders createClientBuilders(Region region, AwsCredentialsProvider credentialsProvider) {
        // One detector per client: operation names are not unique across services, so a shared map could conflate
        // two services' calls.
        final DynamoDbAsyncClientBuilder dynamoDbClientBuilder = DynamoDbAsyncClient.builder();
        awsClientBuilderUtil.initializeBuilder(dynamoDbClientBuilder, request.dynamodbEndpoint(), region, credentialsProvider);
        dynamoDbClientBuilder.httpClientBuilder(awsClientBuilderUtil.asyncHttpClientBuilder());
        dynamoDbClientBuilder.overrideConfiguration(c -> c.addExecutionInterceptor(newAuthorizationFailureDetector()));

        // No detector on CloudWatch: losing metrics is not a reason to fail an input that is otherwise ingesting.
        final CloudWatchAsyncClientBuilder cloudwatchClientBuilder = CloudWatchAsyncClient.builder();
        awsClientBuilderUtil.initializeBuilder(cloudwatchClientBuilder, request.cloudwatchEndpoint(), region, credentialsProvider);
        cloudwatchClientBuilder.httpClientBuilder(awsClientBuilderUtil.asyncHttpClientBuilder());

        // The Kinesis Client Library normally configures the async client through
        // KinesisClientUtil.createKinesisAsyncClient(), but that installs its own HTTP client builder and would discard
        // our proxy configuration. We therefore apply the proxy-aware, HTTP/2-enabled builder ourselves.
        final KinesisAsyncClientBuilder kinesisAsyncClientBuilder = KinesisAsyncClient.builder();
        awsClientBuilderUtil.initializeBuilder(kinesisAsyncClientBuilder, request.kinesisEndpoint(), region, credentialsProvider);
        kinesisAsyncClientBuilder.httpClientBuilder(awsClientBuilderUtil.kinesisAsyncHttpClientBuilder());
        kinesisAsyncClientBuilder.overrideConfiguration(c -> c.addExecutionInterceptor(newAuthorizationFailureDetector()));

        return new ClientBuilders(dynamoDbClientBuilder, cloudwatchClientBuilder, kinesisAsyncClientBuilder);
    }

    private AWSAuthorizationFailureDetector newAuthorizationFailureDetector() {
        return new AWSAuthorizationFailureDetector(this::handleAuthorizationFailure, nanoClock);
    }

    @VisibleForTesting
    record ClientBuilders(DynamoDbAsyncClientBuilder dynamoDb,
                          CloudWatchAsyncClientBuilder cloudWatch,
                          KinesisAsyncClientBuilder kinesis) {
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
     * Clears a transient failure once KCL completes a record-processing task. Terminality is enforced by
     * {@link InputFailureRecorder}, so a task completing concurrently with a terminal failure cannot report the input
     * healthy again.
     */
    @VisibleForTesting
    void recordTaskSuccess() {
        inputFailureRecorder.setRunning();
    }

    /**
     * Fails the input and stops the KCL scheduler after an AWS authorization denial that retrying cannot fix. Without
     * this, KCL retries such calls on a fixed schedule for as long as the input is running, logging an ERROR with a
     * stack trace every time while consuming no records.
     */
    @VisibleForTesting
    void handleAuthorizationFailure(Throwable cause) {
        if (!authorizationFailureHandled.compareAndSet(false, true)) {
            return;
        }
        // Terminal: this message has to replace any earlier transient failure, or the denied action and resource -
        // the only actionable part - stay hidden behind a generic "Errors for Kinesis stream" message forever.
        // The remediation differs for a rejected credential, so the two cases must not share a message.
        final String remedy = AWSAuthorizationFailureDetector.indicatesRejectedCredentials(cause)
                ? "AWS rejected the configured credentials. Correct them, then stop and start the input."
                : "Retrying cannot resolve a missing permission. Grant it, then stop and start the input.";
        inputFailureRecorder.setTerminallyFailing(KinesisConsumer.class, String.format(Locale.ROOT,
                        "AWS authorization failure for Kinesis stream <%s>. Stopping the consumer. %s",
                        kinesisStreamName, remedy), cause);

        // stop() blocks for up to 20s. It runs here on the client's shared SDK response-completion pool
        // (sdk-async-response), and on that pool's rejection path directly on the HTTP thread, so occupying one for
        // that long would stall unrelated completions on the same client.
        final Thread shutdownThread = new Thread(() -> {
            try {
                stop();
            } catch (Exception e) {
                LOG.error("Failed to stop the Kinesis consumer for stream <{}> after an AWS authorization failure.",
                        kinesisStreamName, e);
            }
        }, shutdownThreadName(kinesisStreamName));
        shutdownThread.setDaemon(true);
        shutdownThread.start();
    }

    /**
     * Per-stream so that concurrent failures are distinguishable in a thread dump.
     */
    @VisibleForTesting
    static String shutdownThreadName(String kinesisStreamName) {
        return String.format(Locale.ENGLISH, "aws-kinesis-auth-failure-shutdown-%s", kinesisStreamName);
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
                // Not necessarily a failure: KCL holds its scheduler lock across the whole of its initialization
                // retry loop, so a stop requested while that is still running has to wait for it.
                LOG.warn("Kinesis Consumer for stream <{}> did not shut down within {} seconds. Forcing shutdown, "
                                + "which waits for any in-progress KCL initialization to finish.",
                        kinesisStreamName, GRACEFUL_SHUTDOWN_TIMEOUT);
                kinesisScheduler.shutdown();
            }
        }
    }
}
