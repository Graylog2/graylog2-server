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
package org.graylog.integrations.aws.resources;

import org.graylog.integrations.aws.AWSClientBuilderUtil;
import org.graylog.integrations.aws.resources.requests.CreateLogSubscriptionRequest;
import org.graylog.integrations.aws.resources.requests.CreateRolePermissionRequest;
import org.graylog.integrations.aws.resources.requests.KinesisNewStreamRequest;
import org.graylog.integrations.aws.resources.responses.CreateLogSubscriptionResponse;
import org.graylog.integrations.aws.resources.responses.CreateRolePermissionResponse;
import org.graylog.integrations.aws.resources.responses.KinesisNewStreamResponse;
import org.graylog.integrations.aws.service.CloudWatchService;
import org.graylog.integrations.aws.service.KinesisService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.security.encryption.EncryptedValue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test for all automatic setup requests/responses.
 */
public class KinesisSetupResourceTest {

    private static final String REGION = "us-east-1";
    private static final String KEY = "key";
    private static final String STREAM_NAME = "stream-name";
    private static final String STREAM_ARN = "stream-arn";
    private static final String ROLE_ARN = "role-arn";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private KinesisSetupResource setupResource;
    @Mock
    private IamClient iamClient;
    @Mock
    private CloudWatchLogsClient logsClient;
    @Mock
    private KinesisClient kinesisClient;
    @Mock
    private User currentUser;
    @Mock
    private EncryptedValue encryptedValue;

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
        AWSClientBuilderUtil awsClientBuilderUtil = mock(AWSClientBuilderUtil.class);
        setupResource = new KinesisSetupTestResource(
                new CloudWatchService(mock(CloudWatchLogsClientBuilder.class), awsClientBuilderUtil),
                new KinesisService(mock(IamClientBuilder.class), mock(KinesisClientBuilder.class), null, null, awsClientBuilderUtil));

        when(awsClientBuilderUtil.buildClient(any(IamClientBuilder.class), any())).thenReturn(iamClient);
        when(awsClientBuilderUtil.buildClient(any(CloudWatchLogsClientBuilder.class), any())).thenReturn(logsClient);
        when(awsClientBuilderUtil.buildClient(any(KinesisClientBuilder.class), any())).thenReturn(kinesisClient);

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
                        .awsSecretAccessKey(encryptedValue)
                        .streamName(STREAM_NAME).build();
        final KinesisNewStreamResponse streamResponse = setupResource.createNewKinesisStream(request);
        assertEquals(STREAM_NAME, streamResponse.streamName());
        assertEquals(STREAM_ARN, streamResponse.streamArn());

        // Policy
        final CreateRolePermissionRequest policyRequest =
                CreateRolePermissionRequest.builder()
                        .region(REGION)
                        .awsAccessKeyId(KEY)
                        .awsSecretAccessKey(encryptedValue)
                        .streamName(streamResponse.streamName())
                        .streamArn(streamResponse.streamArn()).build();
        final CreateRolePermissionResponse policyResponse = setupResource.autoKinesisPermissions(policyRequest);
        assertEquals(ROLE_ARN, policyResponse.roleArn());

        // Subscription
        final CreateLogSubscriptionRequest subscriptionRequest =
                CreateLogSubscriptionRequest.builder()
                        .region(REGION)
                        .awsAccessKeyId(KEY)
                        .awsSecretAccessKey(encryptedValue)
                        .logGroupName("log-group-name")
                        .filterName("filter-name")
                        .filterPattern("filter-pattern")
                        .destinationStreamArn(streamResponse.streamArn())
                        .roleArn(policyResponse.roleArn()).build();
        final CreateLogSubscriptionResponse subscriptionResponse = setupResource.createSubscription(subscriptionRequest);
        assertEquals("Success. The subscription filter [filter-name] was added for the CloudWatch log group [log-group-name].",
                subscriptionResponse.result());
    }
}
