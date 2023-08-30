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
package org.graylog.integrations.aws.service;

import org.graylog.integrations.aws.AWSMessageType;
import org.graylog.integrations.aws.codecs.AWSCodec;
import org.graylog.integrations.aws.inputs.AWSInput;
import org.graylog.integrations.aws.resources.requests.AWSInputCreateRequest;
import org.graylog.integrations.aws.resources.responses.AWSRegion;
import org.graylog.integrations.aws.resources.responses.AvailableServiceResponse;
import org.graylog.integrations.aws.resources.responses.KinesisPermissionsResponse;
import org.graylog.integrations.aws.transports.KinesisTransport;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputServiceImpl;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.rest.models.system.inputs.requests.InputCreateRequest;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.amazon.awssdk.regions.Region;

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AWSServiceTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private final NodeId nodeId = new SimpleNodeId("5ca1ab1e-0000-4000-a000-000000000000");

    private AWSService awsService;

    @Mock
    private InputServiceImpl inputService;
    @Mock
    private User user;
    @Mock
    private MessageInput messageInput;
    @Mock
    MessageInputFactory messageInputFactory;
    @Mock
    EncryptedValue encryptedValue;

    @Before
    public void setUp() {

        awsService = new AWSService(inputService, messageInputFactory, nodeId, new ObjectMapperProvider().get());
    }

    @Test
    public void testSaveInput() throws Exception {
        when(inputService.create(isA(HashMap.class))).thenCallRealMethod();
        when(inputService.save(isA(Input.class))).thenReturn("input-id");
        when(user.getName()).thenReturn("a-user-name");
        when(messageInputFactory.create(isA(InputCreateRequest.class), isA(String.class), isA(String.class))).thenReturn(messageInput);

        AWSInputCreateRequest request =
                AWSInputCreateRequest.builder().region(Region.US_EAST_1.id())
                        .awsAccessKeyId("a-key")
                        .awsSecretAccessKey(encryptedValue)
                                     .name("AWS Input")
                                     .awsMessageType(AWSMessageType.KINESIS_CLOUDWATCH_FLOW_LOGS.toString())
                                     .streamName("a-stream")
                                     .batchSize(10000)
                                     .addFlowLogPrefix(true)
                                     .throttlingAllowed(true)
                                     .build();
        awsService.saveInput(request, user);

        // Verify that inputService received a valid input to save.
        final ArgumentCaptor<InputCreateRequest> argumentCaptor = ArgumentCaptor.forClass(InputCreateRequest.class);
        verify(messageInputFactory, times(1)).create(argumentCaptor.capture(), eq("a-user-name"), eq("5ca1ab1e-0000-4000-a000-000000000000"));

        // Just verify that the input create request was prepared correctly. This verifies the important argument
        // transposition logic.
        // It's too hard to mock the full inputService.save process, so we are not going to check the final resulting input.
        InputCreateRequest input = argumentCaptor.getValue();
        assertEquals("AWS Input", input.title());
        assertEquals(AWSInput.TYPE, input.type());
        assertTrue(input.global());
        assertEquals("us-east-1", input.configuration().get(AWSInput.CK_AWS_REGION));
        assertEquals("KINESIS_CLOUDWATCH_FLOW_LOGS", input.configuration().get(AWSCodec.CK_AWS_MESSAGE_TYPE));
        assertEquals("a-key", input.configuration().get(AWSInput.CK_ACCESS_KEY));
        assertEquals(encryptedValue, input.configuration().get(AWSInput.CK_SECRET_KEY));
        assertEquals("us-east-1", input.configuration().get(AWSInput.CK_AWS_REGION));
        assertEquals("a-stream", input.configuration().get(KinesisTransport.CK_KINESIS_STREAM_NAME));
        assertEquals(10000, input.configuration().get(KinesisTransport.CK_KINESIS_RECORD_BATCH_SIZE));
    }

    @Test
    public void regionTest() {

        List<AWSRegion> regions = awsService.getAvailableRegions().regions();

        // Use a loop presence check.
        // Check format of random region.
        boolean foundEuWestRegion = false;
        for (AWSRegion availableAWSRegion : regions) {

            if (availableAWSRegion.regionId().equals("eu-west-2")) {
                foundEuWestRegion = true;
            }
        }
        assertTrue(foundEuWestRegion);
        assertTrue(regions.stream().anyMatch(r -> r.displayValue().equals("Europe (Stockholm): eu-north-1")));
        // AWS periodically adds regions. The number should generally only increase. No need to check exact number.
        assertTrue("There should be at least 34 total regions.", regions.size() >= 34);
    }

    @Test
    public void testAvailableServices() {

        AvailableServiceResponse services = awsService.getAvailableServices();

        // There should be one service.
        assertEquals(1, services.total());
        assertEquals(1, services.services().size());

        // CloudWatch should be in the list of available services.
        assertTrue(services.services().stream().anyMatch(s -> s.name().equals("CloudWatch")));

        // Verify that some of the needed actions are present.
        String policy = services.services().get(0).policy();
        assertTrue(policy.contains("cloudwatch"));
        assertTrue(policy.contains("dynamodb"));
        assertTrue(policy.contains("ec2"));
        assertTrue(policy.contains("elasticloadbalancing"));
        assertTrue(policy.contains("kinesis"));
    }

    @Test
    public void testPermissions() {

        final KinesisPermissionsResponse permissions = awsService.getPermissions();

        // Verify that the setup policy contains some needed permissions.
        assertTrue(permissions.setupPolicy().contains("cloudwatch"));
        assertTrue(permissions.setupPolicy().contains("dynamodb"));
        assertTrue(permissions.setupPolicy().contains("ec2"));
        assertTrue(permissions.setupPolicy().contains("elasticloadbalancing"));
        assertTrue(permissions.setupPolicy().contains("kinesis"));

        // Verify that the auto-setup policy contains some needed permissions.
        assertTrue(permissions.autoSetupPolicy().contains("CreateStream"));
        assertTrue(permissions.autoSetupPolicy().contains("DescribeSubscriptionFilters"));
        assertTrue(permissions.autoSetupPolicy().contains("PutRecord"));
        assertTrue(permissions.autoSetupPolicy().contains("RegisterStreamConsumer"));
    }
}
