package org.graylog.integrations.aws.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.graylog.integrations.aws.AWSMessageType;
import org.graylog.integrations.aws.AWSPolicy;
import org.graylog.integrations.aws.AWSPolicyStatement;
import org.graylog.integrations.aws.codecs.AWSCodec;
import org.graylog.integrations.aws.inputs.AWSInput;
import org.graylog.integrations.aws.resources.requests.AWSInputCreateRequest;
import org.graylog.integrations.aws.resources.responses.AWSRegion;
import org.graylog.integrations.aws.resources.responses.AvailableService;
import org.graylog.integrations.aws.resources.responses.AvailableServiceResponse;
import org.graylog.integrations.aws.resources.responses.KinesisPermissionsResponse;
import org.graylog.integrations.aws.resources.responses.RegionsResponse;
import org.graylog.integrations.aws.transports.KinesisTransport;
import org.graylog2.database.NotFoundException;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.transports.ThrottleableTransport;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.rest.models.system.inputs.requests.InputCreateRequest;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionMetadata;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AWSService {

    private static final Logger LOG = LoggerFactory.getLogger(AWSService.class);

    /**
     * The only version supported is 2012-10-17
     *
     * @see <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies_elements_version.html">IAM JSON Policy Elements: Version</a>
     */
    private static final String AWS_POLICY_VERSION = "2012-10-17";
    public static final String POLICY_ENCODING_ERROR = "An error occurred encoding the policy JSON";

    private final InputService inputService;
    private final MessageInputFactory messageInputFactory;
    private final NodeId nodeId;
    private final ObjectMapper objectMapper;

    @Inject
    public AWSService(InputService inputService, MessageInputFactory messageInputFactory, NodeId nodeId,
                      ObjectMapper objectMapper) {

        this.inputService = inputService;
        this.messageInputFactory = messageInputFactory;
        this.nodeId = nodeId;
        this.objectMapper = objectMapper;
    }

    /**
     * @return A list of all available regions.
     */
    public RegionsResponse getAvailableRegions() {

        List<AWSRegion> regions =
                Region.regions().stream()
                      // Ignore the global region. CloudWatch and Kinesis cannot be used with global regions.
                      .filter(r -> !r.isGlobalRegion())
                      .map(r -> {
                          // Build a single AWSRegionResponse with id, description, and displayValue.
                          RegionMetadata regionMetadata = r.metadata();
                          String label = String.format("%s: %s", regionMetadata.description(), regionMetadata.id());
                          return AWSRegion.create(regionMetadata.id(), label);
                      })
                      .sorted(Comparator.comparing(AWSRegion::regionId))
                      .collect(Collectors.toList());

        return RegionsResponse.create(regions, regions.size());
    }

    /**
     * Build a list of region choices with both a value (persisted in configuration) and display value (shown to the user).
     *
     * The display value is formatted nicely: "EU (London): eu-west-2"
     * The value is eventually passed to Regions.of() to get the actual region object: eu-west-2
     *
     * @return a choices map with configuration value map keys and display value map values.
     */
    public static Map<String, String> buildRegionChoices() {
        Map<String, String> regions = Maps.newHashMap();
        for (Region region : Region.regions()) {

            // Ignore the global region. CloudWatch and Kinesis cannot be used with global regions.
            if (region.isGlobalRegion()) {
                continue;
            }

            RegionMetadata regionMetadata = RegionMetadata.of(region);
            String displayValue = String.format("%s: %s", regionMetadata.description(), region.id());
            regions.put(region.id(), displayValue);
        }
        return regions;
    }

    /**
     * @return A list of available AWS services supported by the AWS Graylog AWS integration.
     */
    public AvailableServiceResponse getAvailableServices() {
        AWSPolicy awsPolicy = buildAwsSetupPolicy();

        ArrayList<AvailableService> services = new ArrayList<>();

        String policy;
        try {
            policy = objectMapper.writeValueAsString(awsPolicy);
        } catch (JsonProcessingException e) {
            LOG.error(POLICY_ENCODING_ERROR, e);
            throw new InternalServerErrorException(POLICY_ENCODING_ERROR, e);
        }
        AvailableService cloudWatchService =
                AvailableService.create("CloudWatch",
                                        "Retrieve CloudWatch logs via Kinesis. Kinesis allows streaming of the logs " +
                                        "in real time. AWS CloudWatch is a monitoring and management service built " +
                                        "for developers, system operators, site reliability engineers (SRE), " +
                                        "and IT managers.",
                                        policy,
                                        "Requires Kinesis",
                                        "https://aws.amazon.com/cloudwatch/");
        services.add(cloudWatchService);
        return AvailableServiceResponse.create(services, services.size());
    }

    /**
     * @return A list of required permissions for the regular AWS Kinesis setup and for the auto-setup.
     */
    public KinesisPermissionsResponse getPermissions() {

        final String setupPolicyString = policyAsJsonString(buildAwsSetupPolicy());
        final String autoSetupPolicyString = policyAsJsonString(buildAwsAutoSetupPolicy());
        return KinesisPermissionsResponse.create(setupPolicyString, autoSetupPolicyString);
    }

    /**
     * Convert the {@link AWSPolicy} object into a JSON string.
     *
     * @return A JSON policy string.
     */
    private String policyAsJsonString(AWSPolicy setupPolicy) {
        try {
            return objectMapper.writeValueAsString(setupPolicy);
        } catch (JsonProcessingException e) {
            // Return a more general internal server error if JSON encoding fails.
            LOG.error(POLICY_ENCODING_ERROR, e);
            throw new InternalServerErrorException(POLICY_ENCODING_ERROR, e);
        }
    }

    /**
     * Create the AWS Kinesis setup policy.
     */
    private AWSPolicy buildAwsSetupPolicy() {
        List<String> actions = Arrays.asList("cloudwatch:PutMetricData",
                                             "dynamodb:CreateTable",
                                             "dynamodb:DescribeTable",
                                             "dynamodb:GetItem",
                                             "dynamodb:PutItem",
                                             "dynamodb:Scan",
                                             "dynamodb:UpdateItem",
                                             "ec2:DescribeInstances",
                                             "ec2:DescribeNetworkInterfaceAttribute",
                                             "ec2:DescribeNetworkInterfaces",
                                             "elasticloadbalancing:DescribeLoadBalancerAttributes",
                                             "elasticloadbalancing:DescribeLoadBalancers",
                                             "iam:CreateRole",
                                             "iam:GetRole",
                                             "iam:PassRole",
                                             "iam:PutRolePolicy",
                                             "kinesis:CreateStream",
                                             "kinesis:DescribeStream",
                                             "kinesis:GetRecords",
                                             "kinesis:GetShardIterator",
                                             "kinesis:ListShards",
                                             "kinesis:ListStreams",
                                             "logs:DescribeLogGroups",
                                             "logs:PutSubscriptionFilter");

        AWSPolicyStatement statement = AWSPolicyStatement.create("GraylogKinesisSetup",
                                                                 "Allow",
                                                                 actions,
                                                                 "*");
        return AWSPolicy.create(AWS_POLICY_VERSION, Collections.singletonList(statement));
    }

    /**
     * Create the AWS Kinesis auto-setup policy.
     */
    private AWSPolicy buildAwsAutoSetupPolicy() {
        List<String> actions = Arrays.asList("iam:PassRole",
                                             "logs:DescribeSubscriptionFilters",
                                             "logs:PutLogEvents",
                                             "kinesis:CreateStream",
                                             "kinesis:DescribeStreamConsumer",
                                             "kinesis:PutRecord",
                                             "kinesis:RegisterStreamConsumer");

        AWSPolicyStatement statement = AWSPolicyStatement.create("GraylogKinesisAutoSetup",
                                                                 "Allow",
                                                                 actions,
                                                                 "*");
        return AWSPolicy.create(AWS_POLICY_VERSION, Collections.singletonList(statement));
    }

    /**
     * Save the AWS Input
     *
     * This method takes the individual input params in the {@link AWSInputCreateRequest} and creates/saves
     * an input with them.
     */
    public Input saveInput(AWSInputCreateRequest request, User user) throws Exception {

        // Transpose the SaveAWSInputRequest to the needed InputCreateRequest
        final HashMap<String, Object> configuration = new HashMap<>();
        configuration.put(AWSCodec.CK_AWS_MESSAGE_TYPE, request.awsMessageType());
        configuration.put(AWSInput.CK_GLOBAL, request.global());
        configuration.put(ThrottleableTransport.CK_THROTTLING_ALLOWED, request.throttlingAllowed());
        configuration.put(AWSCodec.CK_FLOW_LOG_PREFIX, request.addFlowLogPrefix());
        configuration.put(AWSInput.CK_AWS_REGION, request.region());
        configuration.put(AWSInput.CK_ACCESS_KEY, request.awsAccessKeyId());
        configuration.put(AWSInput.CK_SECRET_KEY, request.awsSecretAccessKey());
        configuration.put(AWSInput.CK_ASSUME_ROLE_ARN, request.assumeRoleArn());
        configuration.put(AWSInput.CK_CLOUDWATCH_ENDPOINT, request.cloudwatchEndpoint());
        configuration.put(AWSInput.CK_DYNAMODB_ENDPOINT, request.dynamodbEndpoint());
        configuration.put(AWSInput.CK_IAM_ENDPOINT, request.iamEndpoint());
        configuration.put(AWSInput.CK_KINESIS_ENDPOINT, request.kinesisEndpoint());

        AWSMessageType inputType = AWSMessageType.valueOf(request.awsMessageType());
        if (inputType.isKinesis()) {
            configuration.put(KinesisTransport.CK_KINESIS_STREAM_NAME, request.streamName());
            configuration.put(KinesisTransport.CK_KINESIS_RECORD_BATCH_SIZE, request.batchSize());
        } else {
            throw new Exception("The specified input type is not supported.");
        }

        // Create and save the input.
        final InputCreateRequest inputCreateRequest = InputCreateRequest.create(request.name(),
                                                                                AWSInput.TYPE,
                                                                                false,
                                                                                configuration,
                                                                                nodeId.toString());
        try {
            final MessageInput messageInput = messageInputFactory.create(inputCreateRequest, user.getName(), nodeId.toString());
            messageInput.checkConfiguration();
            final Input input = this.inputService.create(messageInput.asMap());
            final String newInputId = inputService.save(input);
            LOG.debug("New AWS input created. id [{}] request [{}]", newInputId, request);
            return input;
        } catch (NoSuchInputTypeException e) {
            LOG.error("There is no such input type registered.", e);
            throw new NotFoundException("There is no such input type registered.", e);
        } catch (ConfigurationException e) {
            LOG.error("Missing or invalid input configuration.", e);
            throw new BadRequestException("Missing or invalid input configuration.", e);
        }
    }
}