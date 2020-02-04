package org.graylog.integrations.aws.transports;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.graylog.integrations.aws.AWSAuthFactory;
import org.graylog.integrations.aws.AWSClientBuilderUtil;
import org.graylog.integrations.aws.AWSMessageType;
import org.graylog.integrations.aws.resources.requests.AWSRequest;
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
import software.amazon.kinesis.coordinator.Scheduler;
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
    private Scheduler kinesisScheduler;

    KinesisConsumer(NodeId nodeId,
                    KinesisTransport transport,
                    ObjectMapper objectMapper,
                    Consumer<byte[]> handleMessageCallback,
                    String kinesisStreamName,
                    AWSMessageType awsMessageType,
                    int recordBatchSize, AWSRequest request) {
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
    }

    public void run() {

        LOG.debug("Starting the Kinesis Consumer.");
        AwsCredentialsProvider credentialsProvider = AWSAuthFactory.create(request.region(), request.awsAccessKeyId(),
                                                                           request.awsSecretAccessKey(), request.assumeRoleArn());

        final Region region = Region.of(request.region());

        // Create all clients needed for the Kinesis consumer.
        final DynamoDbAsyncClientBuilder dynamoDbClientBuilder = DynamoDbAsyncClient.builder();
        AWSClientBuilderUtil.initializeBuilder(dynamoDbClientBuilder, request.dynamodbEndpoint(), region, credentialsProvider);
        final DynamoDbAsyncClient dynamoClient = dynamoDbClientBuilder.build();

        final CloudWatchAsyncClientBuilder cloudwatchClientBuilder = CloudWatchAsyncClient.builder();
        AWSClientBuilderUtil.initializeBuilder(cloudwatchClientBuilder, request.cloudwatchEndpoint(), region, credentialsProvider);
        final CloudWatchAsyncClient cloudWatchClient = cloudwatchClientBuilder.build();

        final KinesisAsyncClientBuilder kinesisAsyncClientBuilder = KinesisAsyncClient.builder();
        AWSClientBuilderUtil.initializeBuilder(kinesisAsyncClientBuilder, request.kinesisEndpoint(), region, credentialsProvider);
        final KinesisAsyncClient kinesisAsyncClient = KinesisClientUtil.createKinesisAsyncClient(kinesisAsyncClientBuilder);

        final String workerId = String.format(Locale.ENGLISH, "graylog-node-%s", nodeId.anonymize());
        LOG.debug("Using workerId [{}].", workerId);

        // The application name needs to be unique per input/consumer. Using the same name for two different Kinesis
        // streams will cause trouble with state handling in DynamoDB.
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
        // CloudWatch Kinesis subscription records may each contain a large number of log messages.
        // The batch size (max number of messages retrieved in each requests) can be reduced to limit large
        // bursts of messages being dropped onto the Graylog journal. Reducing this value too much can
        // significantly limit throughput.
        if (recordBatchSize != null) {
            LOG.debug("Using explicit batch size [{}]", recordBatchSize);
            pollingConfig.maxRecords(recordBatchSize);
        }
        this.kinesisScheduler = new Scheduler(
                configsBuilder.checkpointConfig(),
                configsBuilder.coordinatorConfig(),
                configsBuilder.leaseManagementConfig(),
                configsBuilder.lifecycleConfig(),
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
            }
        }
    }
}