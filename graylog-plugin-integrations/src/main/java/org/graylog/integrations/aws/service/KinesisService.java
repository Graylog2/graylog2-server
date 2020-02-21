package org.graylog.integrations.aws.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.google.common.base.Preconditions;
import org.apache.commons.collections.CollectionUtils;
import org.graylog.integrations.aws.AWSClientBuilderUtil;
import org.graylog.integrations.aws.AWSLogMessage;
import org.graylog.integrations.aws.AWSMessageType;
import org.graylog.integrations.aws.cloudwatch.CloudWatchLogEvent;
import org.graylog.integrations.aws.cloudwatch.CloudWatchLogSubscriptionData;
import org.graylog.integrations.aws.cloudwatch.KinesisLogEntry;
import org.graylog.integrations.aws.resources.requests.AWSRequest;
import org.graylog.integrations.aws.resources.requests.CreateRolePermissionRequest;
import org.graylog.integrations.aws.resources.requests.KinesisHealthCheckRequest;
import org.graylog.integrations.aws.resources.requests.KinesisNewStreamRequest;
import org.graylog.integrations.aws.resources.responses.CreateRolePermissionResponse;
import org.graylog.integrations.aws.resources.responses.KinesisHealthCheckResponse;
import org.graylog.integrations.aws.resources.responses.KinesisNewStreamResponse;
import org.graylog.integrations.aws.resources.responses.StreamsResponse;
import org.graylog.integrations.aws.transports.KinesisPayloadDecoder;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.IamClientBuilder;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.KinesisClientBuilder;
import software.amazon.awssdk.services.kinesis.model.CreateStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamRequest;
import software.amazon.awssdk.services.kinesis.model.GetRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.GetRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorRequest;
import software.amazon.awssdk.services.kinesis.model.LimitExceededException;
import software.amazon.awssdk.services.kinesis.model.ListShardsRequest;
import software.amazon.awssdk.services.kinesis.model.ListShardsResponse;
import software.amazon.awssdk.services.kinesis.model.ListStreamsRequest;
import software.amazon.awssdk.services.kinesis.model.ListStreamsResponse;
import software.amazon.awssdk.services.kinesis.model.Record;
import software.amazon.awssdk.services.kinesis.model.Shard;
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType;
import software.amazon.awssdk.services.kinesis.model.StreamDescription;
import software.amazon.awssdk.services.kinesis.model.StreamStatus;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.zip.GZIPInputStream;

/**
 * Service for all AWS Kinesis business logic and SDK usages.
 */
public class KinesisService {

    private static final Logger LOG = LoggerFactory.getLogger(AWSService.class);

    private static final int EIGHT_BITS = 8;
    private static final int KINESIS_LIST_STREAMS_MAX_ATTEMPTS = 1000;

    private static final int KINESIS_LIST_STREAMS_LIMIT = 400;
    private static final int RECORDS_SAMPLE_SIZE = 10;
    private static final int SHARD_COUNT = 1;
    private static final String ROLE_NAME_FORMAT = "graylog-cloudwatch-role-%s";
    private static final String ROLE_POLICY_NAME_FORMAT = "graylog-cloudwatch-role-policy-%s";
    private static final String UNIQUE_ROLE_DATE_FORMAT = "yyyy-MM-dd-HH-mm-ss";

    private final IamClientBuilder iamClientBuilder;
    private final KinesisClientBuilder kinesisClientBuilder;
    private final ObjectMapper objectMapper;
    private final Map<String, Codec.Factory<? extends Codec>> availableCodecs;
    private static final String CONTROL_MESSAGE_TOKEN = "CWL CONTROL MESSAGE";

    @Inject
    public KinesisService(IamClientBuilder iamClientBuilder, KinesisClientBuilder kinesisClientBuilder,
                          ObjectMapper objectMapper,
                          Map<String, Codec.Factory<? extends Codec>> availableCodecs) {

        this.iamClientBuilder = iamClientBuilder;
        this.kinesisClientBuilder = kinesisClientBuilder;
        this.objectMapper = objectMapper;
        this.availableCodecs = availableCodecs;
    }

    /**
     * The Health Check performs the following actions:
     * <p>
     * 1) Get all the Kinesis streams.
     * 2) Check if the supplied stream exists.
     * 3) Retrieve one record from Kinesis stream.
     * 4) Check if the payload is compressed.
     * 5) Detect the type of log message.
     * 6) Parse the message if is of a known type.
     *
     * @param request The request, which indicates which stream region to health check
     * @return a {@code KinesisHealthCheckResponse}, which indicates the type of detected message and a sample parsed
     * message.
     */
    public KinesisHealthCheckResponse healthCheck(KinesisHealthCheckRequest request) throws ExecutionException, IOException {

        LOG.debug("Executing healthCheck");
        LOG.debug("Requesting a list of streams to find out if the indicated stream exists.");
        // Get all the Kinesis streams that exist for a user and region
        StreamsResponse kinesisStreamNames = getKinesisStreamNames(request);

        // Check if Kinesis stream exists
        final boolean streamExists = kinesisStreamNames.streams().stream()
                                                       .anyMatch(streamName -> streamName.equals(request.streamName()));
        if (!streamExists) {
            throw new BadRequestException(String.format("The requested stream [%s] was not found.", request.streamName()));
        }

        LOG.debug("The stream [{}] exists", request.streamName());

        KinesisClient kinesisClient = AWSClientBuilderUtil.buildClient(kinesisClientBuilder, request);

        final List<Record> records = retrieveRecords(request.streamName(), kinesisClient);
        if (records.size() == 0) {
            throw new BadRequestException(String.format("The Kinesis stream [%s] does not contain any messages.", request.streamName()));
        }

        Record record = selectRandomRecord(records);
        final byte[] payloadBytes = record.data().asByteArray();
        final boolean compressed = isCompressed(payloadBytes);
        if (compressed) {
            return handleCompressedMessages(request, payloadBytes);
        }

        DateTime timestamp = new DateTime(record.approximateArrivalTimestamp().toEpochMilli(), DateTimeZone.UTC);
        return detectAndParseMessage(new String(payloadBytes), timestamp, request.streamName(), "", "", compressed);
    }

    public StreamsResponse getKinesisStreamNames(AWSRequest request) throws ExecutionException {

        LOG.debug("List Kinesis streams for region [{}]", request.region());

        final KinesisClient kinesisClient = AWSClientBuilderUtil.buildClient(kinesisClientBuilder, request);

        ListStreamsRequest streamsRequest = ListStreamsRequest.builder().limit(KINESIS_LIST_STREAMS_LIMIT).build();
        final ListStreamsResponse listStreamsResponse = kinesisClient.listStreams(streamsRequest);
        final List<String> streamNames = new ArrayList<>(listStreamsResponse.streamNames());

        final Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                .retryIfResult(b -> Objects.equals(b, Boolean.TRUE))
                .retryIfExceptionOfType(LimitExceededException.class)
                .withStopStrategy(StopStrategies.stopAfterAttempt(KINESIS_LIST_STREAMS_MAX_ATTEMPTS))
                .build();

        if (listStreamsResponse.hasMoreStreams()) {
            try {
                retryer.call(() -> {
                    LOG.debug("Requesting streams...");
                    final String lastStreamName = streamNames.get(streamNames.size() - 1);
                    final ListStreamsRequest moreStreamsRequest = ListStreamsRequest.builder()
                                                                                    .exclusiveStartStreamName(lastStreamName)
                                                                                    .limit(KINESIS_LIST_STREAMS_LIMIT).build();
                    final ListStreamsResponse moreSteamsResponse = kinesisClient.listStreams(moreStreamsRequest);
                    streamNames.addAll(moreSteamsResponse.streamNames());

                    // If more streams, then this will execute again.
                    return moreSteamsResponse.hasMoreStreams();
                });
            } catch (RetryException e) {
                LOG.error("Failed to get all stream names after {} attempts. Proceeding to return currently obtained streams.", KINESIS_LIST_STREAMS_MAX_ATTEMPTS);
            }
        }
        LOG.debug("Kinesis streams queried: [{}]", streamNames);

        if (streamNames.isEmpty()) {
            throw new BadRequestException(String.format("No Kinesis streams were found in the [%s] region.", request.region()));
        }

        return StreamsResponse.create(streamNames, streamNames.size());
    }

    /**
     * CloudWatch Kinesis subscription payloads are always compressed. Detecting a compressed payload is currently
     * how the Health Check identifies that the payload has been sent from CloudWatch.
     *
     * @param request      The Health Check request.
     * @param payloadBytes The raw compressed binary payload from Kinesis.
     * @return a {@code KinesisHealthCheckResponse}, which indicates the type of detected message and a sample parsed
     * message.
     * @see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/SubscriptionFilters.html"/>
     */
    private KinesisHealthCheckResponse handleCompressedMessages(KinesisHealthCheckRequest request, byte[] payloadBytes) throws IOException {
        LOG.debug("The supplied payload is GZip compressed. Proceeding to decompress.");

        // Assume that the payload is from CloudWatch.
        final CloudWatchLogSubscriptionData data = KinesisPayloadDecoder.decompressCloudWatchMessages(payloadBytes, objectMapper);

        // Pick just one log entry.
        Optional<CloudWatchLogEvent> logEntryOptional = data.logEvents().stream().findAny();

        if (!logEntryOptional.isPresent()) {
            throw new BadRequestException("The CloudWatch payload did not contain any messages. This should not happen. " +
                                          "See https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/SubscriptionFilters.html");
        }

        CloudWatchLogEvent logEntry = logEntryOptional.get();
        DateTime timestamp = new DateTime(logEntry.timestamp(), DateTimeZone.UTC);
        return detectAndParseMessage(logEntry.message(), timestamp,
                                     request.streamName(), data.logGroup(), data.logStream(), true);
    }

    /**
     * Get a list of Records that exists in a Kinesis stream.
     *
     * @param kinesisStream The name of the Kinesis stream
     * @param kinesisClient The KinesClient interface
     * @return A sample size of records (between 0-5 records) in a Kinesis stream
     */
    List<Record> retrieveRecords(String kinesisStream, KinesisClient kinesisClient) {

        LOG.debug("About to retrieve logs records from Kinesis.");
        // Create ListShard request and response and designate the Kinesis stream
        final ListShardsRequest listShardsRequest = ListShardsRequest.builder().streamName(kinesisStream).build();
        final ListShardsResponse listShardsResponse = kinesisClient.listShards(listShardsRequest);
        final List<Record> recordsList = new ArrayList<>();

        // Iterate through the shards that exist
        for (Shard shard : listShardsResponse.shards()) {
            final String shardId = shard.shardId();
            final GetShardIteratorRequest getShardIteratorRequest =
                    GetShardIteratorRequest.builder()
                                           .shardId(shardId)
                                           .streamName(kinesisStream)
                                           .shardIteratorType(ShardIteratorType.TRIM_HORIZON)
                                           .build();
            String shardIterator = kinesisClient.getShardIterator(getShardIteratorRequest).shardIterator();
            boolean stayOnCurrentShard = true;
            LOG.debug("Retrieved shard id: [{}] with shard iterator: [{}]", shardId, shardIterator);
            while (stayOnCurrentShard) {
                LOG.debug("Getting more records");
                final GetRecordsRequest getRecordsRequest = GetRecordsRequest.builder().shardIterator(shardIterator).build();
                final GetRecordsResponse getRecordsResponse = kinesisClient.getRecords(getRecordsRequest);
                shardIterator = getRecordsResponse.nextShardIterator();

                for (Record record : getRecordsResponse.records()) {
                    if (isControlMessage(record)) {
                        continue;
                    }
                    recordsList.add(record);

                    if (recordsList.size() == RECORDS_SAMPLE_SIZE) {
                        LOG.debug("Returning the list of records now that sample size [{}] has been met.", RECORDS_SAMPLE_SIZE);
                        return recordsList;
                    }
                }

                if (getRecordsResponse.millisBehindLatest() == 0) {
                    LOG.debug("Found the end of the shard. No more records returned from the shard.");
                    stayOnCurrentShard = false;
                }
            }
        }
        LOG.debug("Returning the list with [{}] records.", recordsList.size());
        return recordsList;
    }

    /**
     * Skip messages that contain the CloudWatch control token (CWL CONTROL MESSAGE).
     * These messages are automatically written by CloudWatch when the CloudWatch log subscription is
     * created in order to test the subscription (and can be safely ignored).
     *
     * @return true if the message contains
     */
    private boolean isControlMessage(Record record) {
        final byte[] recordData = record.data().asByteArray();
        if (isCompressed(recordData)) {
            try {
                return Tools.decompressGzip(recordData).contains(CONTROL_MESSAGE_TOKEN);
            } catch (IOException e) {
                throw new BadRequestException("Failed to decode message from CloudWatch and check if it's a control message.");
            }
        }
        return false;
    }

    /**
     * Detect the message type.
     *
     * @param logMessage        A string containing the actual log message.
     * @param timestamp         The message timestamp.
     * @param kinesisStreamName The stream name.
     * @param logGroupName      The CloudWatch log group name.
     * @param logStreamName     The CloudWatch log stream name.
     * @param compressed        Indicates if the payload is compressed and probably from CloudWatch.
     * @return A {@code KinesisHealthCheckResponse} with the fully parsed message and type.
     */
    private KinesisHealthCheckResponse detectAndParseMessage(String logMessage, DateTime timestamp, String kinesisStreamName,
                                                             String logGroupName, String logStreamName, boolean compressed) {

        LOG.debug("Attempting to detect the type of log message. message [{}] stream [{}] log group [{}].",
                  logMessage, kinesisStreamName, logGroupName);

        final AWSLogMessage awsLogMessage = new AWSLogMessage(logMessage);
        AWSMessageType awsMessageType = awsLogMessage.detectLogMessageType(compressed);

        LOG.debug("The message is type [{}]", awsMessageType);

        final String responseMessage = String.format("Success. The message is a %s message.", awsMessageType.getLabel());

        final KinesisLogEntry logEvent = KinesisLogEntry.create(kinesisStreamName, logGroupName, logStreamName,
                                                                timestamp, logMessage);

        final Codec.Factory<? extends Codec> codecFactory = this.availableCodecs.get(awsMessageType.getCodecName());
        if (codecFactory == null) {
            throw new BadRequestException(String.format("A codec with name [%s] could not be found.", awsMessageType.getCodecName()));
        }

        // TODO: Do we need to provide a valid configuration here?
        final Codec codec = codecFactory.create(Configuration.EMPTY_CONFIGURATION);

        final byte[] payload;
        try {
            payload = objectMapper.writeValueAsBytes(logEvent);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Encoding the message to bytes failed.", e);
        }

        final Message fullyParsedMessage = codec.decode(new RawMessage(payload));
        if (fullyParsedMessage == null) {
            throw new BadRequestException(String.format("Message decoding failed. More information might be " +
                                                        "available by enabling Debug logging. message [%s]", logMessage));
        }

        LOG.debug("Successfully parsed message type [{}] with codec [{}].", awsMessageType, awsMessageType.getCodecName());

        return KinesisHealthCheckResponse.create(awsMessageType, responseMessage, fullyParsedMessage.getFields());
    }

    Record selectRandomRecord(List<Record> recordsList) {

        Preconditions.checkArgument(CollectionUtils.isNotEmpty(recordsList), "Records list can not be empty.");

        LOG.debug("Selecting a random Record from the sample list.");
        return recordsList.get(new Random().nextInt(recordsList.size()));
    }

    /**
     * Checks if the supplied stream is GZip compressed.
     *
     * @param bytes a byte array.
     * @return true if the byte array is GZip compressed and false if not.
     */
    public static boolean isCompressed(byte[] bytes) {
        if ((bytes == null) || (bytes.length < 2)) {
            return false;
        } else {

            // If the byte array is GZipped, then the first or second byte will be the GZip magic number.
            final boolean firstByteIsMagicNumber = bytes[0] == (byte) (GZIPInputStream.GZIP_MAGIC);
            // The >> operator shifts the GZIP magic number to the second byte.
            final boolean secondByteIsMagicNumber = bytes[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> EIGHT_BITS);
            return firstByteIsMagicNumber && secondByteIsMagicNumber;
        }
    }

    /**
     * Creates a new Kinesis stream.
     *
     * @param request request which contains region, access, secret, region, streamName and shardCount
     * @return the status response
     */
    public KinesisNewStreamResponse createNewKinesisStream(KinesisNewStreamRequest request) {
        LOG.debug("Creating Kinesis client with the provided credentials.");
        final KinesisClient kinesisClient = AWSClientBuilderUtil.buildClient(kinesisClientBuilder, request);

        LOG.debug("Creating new Kinesis stream request [{}].", request.streamName());
        final CreateStreamRequest createStreamRequest = CreateStreamRequest.builder()
                                                                           .streamName(request.streamName())
                                                                           .shardCount(SHARD_COUNT)
                                                                           .build();
        LOG.debug("Sending request to create new Kinesis stream [{}] with [{}] shards.",
                  request.streamName(), SHARD_COUNT);

        StreamDescription streamDescription;
        try {
            kinesisClient.createStream(createStreamRequest);
            int seconds = 0;
            do {
                try {
                    Thread.sleep(1_000);
                } catch (InterruptedException e) {
                    LOG.error("Request interrupted while waiting for shard to become available.");
                    return null; // Give up on request.
                }
                streamDescription = kinesisClient
                        .describeStream(DescribeStreamRequest.builder().streamName(request.streamName()).build())
                        .streamDescription();
                if (seconds > 300) {
                    final String responseMessage = String.format("Fail. Stream [%s] has failed to become active " +
                                                                 "within 60 seconds.", request.streamName());
                    throw new BadRequestException(responseMessage);
                }
                seconds++;
            } while (streamDescription.streamStatus() != StreamStatus.ACTIVE);
            String streamArn = streamDescription.streamARN();
            final String responseMessage = String.format("Success. The new stream [%s/%s] was created with [%d] shard.",
                                                         request.streamName(), streamArn, SHARD_COUNT);

            return KinesisNewStreamResponse.create(createStreamRequest.streamName(),
                                                   streamArn,
                                                   responseMessage);
        } catch (Exception e) {
            final String specificError = ExceptionUtils.formatMessageCause(e);
            final String responseMessage = String.format("Attempt to create [%s] new Kinesis stream " +
                                                         "with [%d] shards failed due to the following exception: [%s]",
                                                         request.streamName(), SHARD_COUNT,
                                                         specificError);
            LOG.error(responseMessage, e);
            throw new BadRequestException(responseMessage, e);
        }
    }

    /**
     * Creates and sets the new role and permissions for Kinesis to talk to Cloudwatch.
     *
     * @param request The create permission request.
     * @return role Arn associated with the associated kinesis stream
     */
    public CreateRolePermissionResponse autoKinesisPermissions(CreateRolePermissionRequest request) {

        String roleName = String.format(ROLE_NAME_FORMAT, DateTime.now().toString(UNIQUE_ROLE_DATE_FORMAT));
        try {
            final IamClient iamClient = AWSClientBuilderUtil.buildClient(iamClientBuilder, request);
            String createRoleResponse = createRoleForKinesisAutoSetup(iamClient, request.region(), roleName);
            LOG.debug(createRoleResponse);
            setPermissionsForKinesisAutoSetupRole(iamClient, roleName, request.streamArn());

            final String roleArn = getRolePermissionsArn(iamClient, roleName);
            final String explanation = String.format("Success! The role [%s/%s] has been created.", roleName, roleArn);
            return CreateRolePermissionResponse.create(explanation, roleArn, roleName);

        } catch (Exception e) {
            final String specificError = ExceptionUtils.formatMessageCause(e);
            final String responseMessage = String.format("Unable to automatically set up Kinesis role [%s] due to the " +
                                                         "following error [%s]", roleName,
                                                         specificError);
            throw new BadRequestException(responseMessage);
        }
    }

    private static void setPermissionsForKinesisAutoSetupRole(IamClient iam, String roleName, String streamArn) {
        String rolePolicy =
                "{\n" +
                "  \"Statement\": [\n" +
                "    {\n" +
                "      \"Effect\": \"Allow\",\n" +
                "      \"Action\": \"kinesis:PutRecord\",\n" +
                "      \"Resource\": \"" + streamArn + "\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        final String rolePolicyName = String.format(ROLE_POLICY_NAME_FORMAT, DateTime.now().toString(UNIQUE_ROLE_DATE_FORMAT));
        LOG.debug("Attaching [{}] policy to [{}] role", rolePolicyName, roleName);
        try {
            iam.putRolePolicy(r -> r.roleName(roleName).policyName(rolePolicyName).policyDocument(rolePolicy));
            LOG.debug("Success! The role policy [{}] was assigned.", rolePolicyName);
        } catch (Exception e) {
            final String specificError = ExceptionUtils.formatMessageCause(e);
            final String responseMessage = String.format("Unable to create role [%s] due to the " +
                                                         "following error [%s]", roleName, specificError);
            throw new BadRequestException(responseMessage);
        }
    }

    private static String createRoleForKinesisAutoSetup(IamClient iam, String region, String roleName) {

        // Create unique role name in this format "graylog-cloudwatch-role-2019-08-08-07-35-34"
        LOG.debug("Create Kinesis Auto Setup Role [{}] to region [{}]", roleName, region);
        String assumeRolePolicy =
                "{\n" +
                "  \"Statement\": [\n" +
                "    {\n" +
                "      \"Effect\": \"Allow\",\n" +
                "      \"Principal\": { \"Service\": \"logs." + region + ".amazonaws.com\" },\n" +
                "      \"Action\": \"sts:AssumeRole\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        // TODO optimize checking if the role exists first
        LOG.debug("Role [{}] was created.", roleName);
        try {
            iam.createRole(r -> r.roleName(roleName).assumeRolePolicyDocument(assumeRolePolicy));
            return String.format("Success! The role [%s] was created.", roleName);
        } catch (Exception e) {
            final String specificError = ExceptionUtils.formatMessageCause(e);
            final String responseMessage = String.format("The role [%s] was not created due to the " +
                                                         "following reason [%s]", roleName, specificError);
            throw new BadRequestException(responseMessage);
        }
    }

    private static String getRolePermissionsArn(IamClient iamClient, String roleName) {
        LOG.debug("Acquiring the role ARN associated to the role [{}]", roleName);
        return iamClient.getRole(r -> r.roleName(roleName)).role().arn();
    }
}