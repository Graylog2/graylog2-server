package org.graylog.integrations.aws.resources;

import org.graylog.integrations.aws.resources.requests.CreateLogSubscriptionRequest;
import org.graylog.integrations.aws.resources.requests.CreateRolePermissionRequest;
import org.graylog.integrations.aws.resources.requests.KinesisNewStreamRequest;
import org.graylog.integrations.aws.resources.responses.CreateLogSubscriptionResponse;
import org.graylog.integrations.aws.resources.responses.CreateRolePermissionResponse;
import org.graylog.integrations.aws.resources.responses.KinesisNewStreamResponse;
import org.graylog.integrations.aws.service.CloudWatchService;
import org.graylog.integrations.aws.service.KinesisService;
import org.graylog2.plugin.database.users.User;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClientBuilder;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutSubscriptionFilterRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutSubscriptionFilterResponse;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.IamClientBuilder;
import software.amazon.awssdk.services.iam.model.CreateRoleResponse;
import software.amazon.awssdk.services.iam.model.GetRoleResponse;
import software.amazon.awssdk.services.iam.model.PutRolePolicyResponse;
import software.amazon.awssdk.services.iam.model.Role;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.KinesisClientBuilder;
import software.amazon.awssdk.services.kinesis.model.CreateStreamRequest;
import software.amazon.awssdk.services.kinesis.model.CreateStreamResponse;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamResponse;
import software.amazon.awssdk.services.kinesis.model.StreamDescription;
import software.amazon.awssdk.services.kinesis.model.StreamStatus;

import javax.annotation.Nullable;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

/**
 * Integration test for all automatic setup requests/responses.
 */
public class KinesisSetupResourceTest {

    private static final String REGION = "us-east-1";
    private static final String KEY = "key";
    private static final String SECRET = "secret";
    private static final String STREAM_NAME = "stream-name";
    private static final String STREAM_ARN = "stream-arn";
    private static final String ROLE_ARN = "role-arn";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private KinesisSetupResource setupResource;

    private CloudWatchService cloudWatchService;
    private KinesisService kinesisService;

    @Mock
    private IamClientBuilder iamClientBuilder = Mockito.mock(IamClientBuilder.class);

    @Mock
    private IamClient iamClient = Mockito.mock(IamClient.class);

    @Mock
    private CloudWatchLogsClientBuilder logsClientBuilder = Mockito.mock(CloudWatchLogsClientBuilder.class);

    @Mock
    private CloudWatchLogsClient logsClient = Mockito.mock(CloudWatchLogsClient.class);

    @Mock
    private KinesisClientBuilder kinesisClientBuilder = Mockito.mock(KinesisClientBuilder.class);

    @Mock
    private KinesisClient kinesisClient = Mockito.mock(KinesisClient.class);

    @Mock
    private User currentUser;

    /**
     * Provide override getCurrentUser for test context since Principal is not established by default.
     */
    class KinesisSetupTestResource extends KinesisSetupResource {

        KinesisSetupTestResource(CloudWatchService cloudWatchService, KinesisService kinesisService) {
            super(cloudWatchService, kinesisService);
        }

        @Nullable
        @Override
        protected User getCurrentUser() {
            return currentUser;
        }
    }

    @Before
    public void setUp() {

        // Set up services.
        cloudWatchService = new CloudWatchService(logsClientBuilder);
        kinesisService = new KinesisService(iamClientBuilder, kinesisClientBuilder, null, null);
        setupResource = new KinesisSetupTestResource(cloudWatchService, kinesisService);

        // Set up IAM client.
        when(iamClientBuilder.region(isA(Region.class))).thenReturn(iamClientBuilder);
        when(iamClientBuilder.credentialsProvider(isA(AwsCredentialsProvider.class))).thenReturn(iamClientBuilder);
        when(iamClientBuilder.build()).thenReturn(iamClient);

        // Set up CloudWatch client.
        when(logsClientBuilder.region(isA(Region.class))).thenReturn(logsClientBuilder);
        when(logsClientBuilder.credentialsProvider(isA(AwsCredentialsProvider.class))).thenReturn(logsClientBuilder);
        when(logsClientBuilder.build()).thenReturn(logsClient);

        // Set up Kinesis client.
        when(kinesisClientBuilder.region(isA(Region.class))).thenReturn(kinesisClientBuilder);
        when(kinesisClientBuilder.credentialsProvider(isA(AwsCredentialsProvider.class))).thenReturn(kinesisClientBuilder);
        when(kinesisClientBuilder.build()).thenReturn(kinesisClient);

        // Stream AWS request mocks
        when(kinesisClient.createStream(isA(CreateStreamRequest.class)))
                .thenReturn(CreateStreamResponse.builder().build());
        when(kinesisClient.describeStream(isA(DescribeStreamRequest.class)))
                .thenReturn(DescribeStreamResponse.builder().streamDescription(StreamDescription.builder()
                                                                                                .streamName(STREAM_NAME)
                                                                                                .streamStatus(StreamStatus.ACTIVE)
                                                                                                .streamARN(STREAM_ARN)
                                                                                                .build()).build());

        // Policy AWS request mocks
        when(iamClient.createRole(isA(Consumer.class)))
                .thenReturn(CreateRoleResponse.builder().role(Role.builder().build()).build());
        when(iamClient.putRolePolicy(isA(Consumer.class)))
                .thenReturn(PutRolePolicyResponse.builder().build());
        when(iamClient.getRole(isA(Consumer.class)))
                .thenReturn(GetRoleResponse.builder().role(Role.builder().arn(ROLE_ARN).build()).build());

        // Subscription AWS request mocks
        when(logsClient.putSubscriptionFilter(isA(PutSubscriptionFilterRequest.class)))
                .thenReturn(PutSubscriptionFilterResponse.builder().build());
    }

    @Test
    public void testAll() {

        // Stream
        final KinesisNewStreamRequest request =
                KinesisNewStreamRequest.builder()
                                       .region(Region.EU_WEST_1.id())
                                       .awsAccessKeyId(KEY)
                                       .awsSecretAccessKey(SECRET)
                                       .streamName(STREAM_NAME).build();
        final KinesisNewStreamResponse streamResponse = setupResource.createNewKinesisStream(request);
        assertEquals(STREAM_NAME, streamResponse.streamName());
        assertEquals(STREAM_ARN, streamResponse.streamArn());

        // Policy
        final CreateRolePermissionRequest policyRequest =
                CreateRolePermissionRequest.builder()
                                           .region(REGION)
                                           .awsAccessKeyId(KEY)
                                           .awsSecretAccessKey(SECRET)
                                           .streamName(streamResponse.streamName())
                                           .streamArn(streamResponse.streamArn()).build();
        final CreateRolePermissionResponse policyResponse = setupResource.autoKinesisPermissions(policyRequest);
        assertEquals(ROLE_ARN, policyResponse.roleArn());

        // Subscription
        final CreateLogSubscriptionRequest subscriptionRequest =
                CreateLogSubscriptionRequest.builder()
                                            .region(REGION)
                                            .awsAccessKeyId(KEY)
                                            .awsSecretAccessKey(SECRET)
                                            .logGroupName("log-group-name")
                                            .filterName("filter-name")
                                            .filterPattern("filter-pattern")
                                            .destinationStreamArn(streamResponse.streamArn())
                                            .roleArn(policyResponse.roleArn()).build();
        final CreateLogSubscriptionResponse subscriptionResponse = setupResource.createSubscription(subscriptionRequest);
        subscriptionResponse.result();
        assertEquals("Success. The subscription filter [filter-name] was added for the CloudWatch log group [log-group-name].",
                     subscriptionResponse.result());
    }
}
