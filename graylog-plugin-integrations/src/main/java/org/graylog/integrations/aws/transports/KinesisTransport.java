package org.graylog.integrations.aws.transports;

import com.codahale.metrics.MetricSet;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.assistedinject.Assisted;
import org.apache.commons.lang3.StringUtils;
import org.graylog.integrations.aws.AWSMessageType;
import org.graylog.integrations.aws.codecs.AWSCodec;
import org.graylog.integrations.aws.inputs.AWSInput;
import org.graylog.integrations.aws.resources.requests.AWSRequest;
import org.graylog.integrations.aws.resources.requests.AWSRequestImpl;
import org.graylog.integrations.aws.service.AWSService;
import org.graylog2.plugin.LocalMetricRegistry;
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
import org.graylog2.plugin.system.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;

import javax.inject.Inject;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class KinesisTransport extends ThrottleableTransport {
    private static final Logger LOG = LoggerFactory.getLogger(KinesisTransport.class);
    public static final String NAME = "aws-kinesis-transport";

    private static final String CK_AWS_REGION = "aws_region";
    private static final String CK_ACCESS_KEY = "aws_access_key";
    private static final String CK_SECRET_KEY = "aws_secret_key";
    public static final String CK_KINESIS_STREAM_NAME = "kinesis_stream_name";
    public static final String CK_KINESIS_RECORD_BATCH_SIZE = "kinesis_record_batch_size";

    public static final int DEFAULT_BATCH_SIZE = 10000;

    private final Configuration configuration;
    private final NodeId nodeId;
    private final LocalMetricRegistry localRegistry;
    private final ObjectMapper objectMapper;

    private KinesisConsumer kinesisConsumer;
    private final ExecutorService executor;

    @Inject
    public KinesisTransport(@Assisted final Configuration configuration,
                            EventBus serverEventBus,
                            final NodeId nodeId,
                            LocalMetricRegistry localRegistry,
                            ObjectMapper objectMapper) {
        super(serverEventBus, configuration);
        this.configuration = configuration;
        this.nodeId = nodeId;
        this.localRegistry = localRegistry;
        this.objectMapper = objectMapper;
        this.executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                                                                  .setDaemon(true)
                                                                  .setNameFormat("aws-kinesis-reader-%d")
                                                                  .setUncaughtExceptionHandler((t, e) -> LOG.error("Uncaught exception in AWS Kinesis reader.", e))
                                                                  .build());
    }

    @Override
    public void handleChangedThrottledState(boolean isThrottled) {

        // Deliberately info level, so it is obvious when throttling occurs.
        if (!isThrottled) {
            LOG.info("Kinesis consumer unthrottled");
        } else {
            LOG.info("Kinesis consumer throttled");
        }
    }

    @Override
    public void doLaunch(MessageInput input) throws MisfireException {

        final Region region = Region.of(Objects.requireNonNull(configuration.getString(CK_AWS_REGION)));
        final String key = configuration.getString(CK_ACCESS_KEY);
        final String secret = configuration.getString(CK_SECRET_KEY);
        final String assumeRoleArn = configuration.getString(AWSInput.CK_ASSUME_ROLE_ARN);

        final String dynamodbEndpoint = configuration.getString(AWSInput.CK_DYNAMODB_ENDPOINT);
        final String cloudwatchEndpoint = configuration.getString(AWSInput.CK_CLOUDWATCH_ENDPOINT);
        final String iamEndpoint = configuration.getString(AWSInput.CK_IAM_ENDPOINT);
        final String kinesisEndpoint = configuration.getString(AWSInput.CK_KINESIS_ENDPOINT);

        // Validate the endpoints, because the Kinesis client will silently fail if an endpoint is invalid.
        validateEndpoint(dynamodbEndpoint, "DynamoDB");
        validateEndpoint(cloudwatchEndpoint, "CloudWatch");
        validateEndpoint(iamEndpoint, "IAM");
        validateEndpoint(iamEndpoint, "Kinesis");

        final AWSRequest awsRequest = AWSRequestImpl.builder()
                                                    .region(region.id())
                                                    .awsAccessKeyId(key)
                                                    .awsSecretAccessKey(secret)
                                                    .assumeRoleArn(assumeRoleArn)
                                                    .cloudwatchEndpoint(cloudwatchEndpoint)
                                                    .dynamodbEndpoint(dynamodbEndpoint)
                                                    .iamEndpoint(iamEndpoint)
                                                    .kinesisEndpoint(kinesisEndpoint).build();

        final int batchSize = configuration.getInt(CK_KINESIS_RECORD_BATCH_SIZE, DEFAULT_BATCH_SIZE);
        final String streamName = configuration.getString(CK_KINESIS_STREAM_NAME);
        final AWSMessageType awsMessageType = AWSMessageType.valueOf(configuration.getString(AWSCodec.CK_AWS_MESSAGE_TYPE));

        this.kinesisConsumer = new KinesisConsumer(nodeId, this, objectMapper, kinesisCallback(input),
                                                   streamName, awsMessageType, batchSize, awsRequest);

        LOG.debug("Starting Kinesis reader thread for input [{}/{}]", input.getName(), input.getId());
        executor.submit(this.kinesisConsumer);
    }

    /**
     * If specified, verify that the specified endpoint is a valid URI.
     *
     * @param endpoint     The endpoint URI.
     * @param endpointName The name of the endpoint. This will be included in the exception message.
     * @throws MisfireException A {@link MisfireException} will be thrown if the URI is invalid.
     */
    static void validateEndpoint(String endpoint, String endpointName) throws MisfireException {
        if (StringUtils.isNotEmpty(endpoint)) {
            try {
                new URL(endpoint).toURI();
            } catch (Exception e) {
                // Re-throw the exception to fail the input start attempt
                throw new MisfireException(String.format("The specified [%s] Override Endpoint [%s] is invalid.",
                                                         endpointName, endpoint), e);
            }
        }
    }

    private Consumer<byte[]> kinesisCallback(final MessageInput input) {
        return (data) -> input.processRawMessage(new RawMessage(data));
    }

    @Override
    public void doStop() {
        if (this.kinesisConsumer != null) {
            this.kinesisConsumer.stop();
        }
    }

    @Override
    public void setMessageAggregator(CodecAggregator aggregator) {
        // Not supported.
    }

    @Override
    public MetricSet getMetricSet() {
        return localRegistry;
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<KinesisTransport> {
        @Override
        KinesisTransport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends ThrottleableTransport.Config {

        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest r = super.getRequestedConfiguration();

            r.addField(new DropdownField(
                    CK_AWS_REGION,
                    "AWS Region",
                    Region.US_EAST_1.id(),
                    AWSService.buildRegionChoices(),
                    "The AWS region the Kinesis stream is running in.",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            r.addField(new TextField(
                    CK_ACCESS_KEY,
                    "AWS access key",
                    "",
                    "Access key of an AWS user with sufficient permissions. (See documentation)",
                    ConfigurationField.Optional.OPTIONAL
            ));

            r.addField(new TextField(
                    CK_SECRET_KEY,
                    "AWS secret key",
                    "",
                    "Secret key of an AWS user with sufficient permissions. (See documentation)",
                    ConfigurationField.Optional.OPTIONAL,
                    TextField.Attribute.IS_PASSWORD
            ));

            r.addField(new TextField(
                    CK_KINESIS_STREAM_NAME,
                    "Kinesis Stream name",
                    "",
                    "The name of the Kinesis stream that receives your messages. See README for instructions on how to connect messages to a Kinesis Stream.",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            r.addField(new NumberField(
                    CK_KINESIS_RECORD_BATCH_SIZE,
                    "Kinesis Record batch size.",
                    DEFAULT_BATCH_SIZE,
                    "The number of Kinesis records to fetch at a time. Each record may be up to 1MB in size. The AWS default is 10,000. Enter a smaller value to process smaller chunks at a time.",
                    ConfigurationField.Optional.OPTIONAL,
                    NumberField.Attribute.ONLY_POSITIVE));

            return r;
        }
    }
}