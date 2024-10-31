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
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.graylog.integrations.aws.AWSClientBuilderUtil;
import org.graylog.integrations.aws.AWSMessageType;
import org.graylog.integrations.aws.resources.requests.AWSRequest;
import org.graylog2.plugin.InputFailureRecorder;
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
import software.amazon.kinesis.common.ConfigsBuilder;
import software.amazon.kinesis.common.KinesisClientUtil;
import software.amazon.kinesis.coordinator.NoOpWorkerStateChangeListener;
import software.amazon.kinesis.coordinator.Scheduler;
import software.amazon.kinesis.coordinator.WorkerStateChangeListener;
import software.amazon.kinesis.lifecycle.NoOpTaskExecutionListener;
import software.amazon.kinesis.lifecycle.TaskExecutionListener;
import software.amazon.kinesis.lifecycle.TaskOutcome;
import software.amazon.kinesis.lifecycle.TaskType;
import software.amazon.kinesis.lifecycle.events.TaskExecutionListenerInput;
import software.amazon.kinesis.retrieval.polling.PollingConfig;

import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
    private final Consumer<byte[]> handleMessageCallback;
    private final AWSRequest request;
    private final AWSClientBuilderUtil awsClientBuilderUtil;
    private final InputFailureRecorder inputFailureRecorder;
    private Scheduler kinesisScheduler;

    KinesisConsumer(NodeId nodeId,
                    KinesisTransport transport,
                    ObjectMapper objectMapper,
                    Consumer<byte[]> handleMessageCallback,
                    String kinesisStreamName,
                    AWSMessageType awsMessageType,
                    int recordBatchSize, AWSRequest request,
                    AWSClientBuilderUtil awsClientBuilderUtil,
                    InputFailureRecorder inputFailureRecorder) {
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
    }

    @Override
    public void run() {

        LOG.debug("Starting the Kinesis Consumer.");
        AwsCredentialsProvider credentialsProvider = awsClientBuilderUtil.createCredentialsProvider(request);

        final Region region = Region.of(request.region());

        final DynamoDbAsyncClientBuilder dynamoDbClientBuilder = DynamoDbAsyncClient.builder();
        awsClientBuilderUtil.initializeBuilder(dynamoDbClientBuilder, request.dynamodbEndpoint(), region, credentialsProvider);
        final DynamoDbAsyncClient dynamoClient = dynamoDbClientBuilder.build();

        final CloudWatchAsyncClientBuilder cloudwatchClientBuilder = CloudWatchAsyncClient.builder();
        awsClientBuilderUtil.initializeBuilder(cloudwatchClientBuilder, request.cloudwatchEndpoint(), region, credentialsProvider);
        final CloudWatchAsyncClient cloudWatchClient = cloudwatchClientBuilder.build();

        final KinesisAsyncClientBuilder kinesisAsyncClientBuilder = KinesisAsyncClient.builder();
        awsClientBuilderUtil.initializeBuilder(kinesisAsyncClientBuilder, request.kinesisEndpoint(), region, credentialsProvider);
        final KinesisAsyncClient kinesisAsyncClient = KinesisClientUtil.createKinesisAsyncClient(kinesisAsyncClientBuilder);

        final String workerId = String.format(Locale.ENGLISH, "graylog-node-%s", nodeId.anonymize());
        LOG.debug("Using workerId [{}].", workerId);

        // The application name needs to be unique per input/consumer.
        final String applicationName = String.format(Locale.ENGLISH, "graylog-aws-plugin-%s", kinesisStreamName);
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
                    inputFailureRecorder.setRunning();
                }
            }
        };

        this.kinesisScheduler = new Scheduler(
                configsBuilder.checkpointConfig(),
                configsBuilder.coordinatorConfig().workerStateChangeListener(workerStateChangeListener),
                configsBuilder.leaseManagementConfig(),
                configsBuilder.lifecycleConfig().taskExecutionListener(taskExecutionListener),
                configsBuilder.metricsConfig(),
                configsBuilder.processorConfig(),
                configsBuilder.retrievalConfig().retrievalSpecificConfig(pollingConfig));

        LOG.debug("Starting Kinesis scheduler.");
        kinesisScheduler.run();
        LOG.debug("After Kinesis scheduler stopped.");
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
