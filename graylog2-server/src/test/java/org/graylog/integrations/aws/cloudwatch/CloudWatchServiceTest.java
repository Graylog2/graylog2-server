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
package org.graylog.integrations.aws.cloudwatch;

import org.graylog.integrations.aws.AWSClientBuilderUtil;
import org.graylog.integrations.aws.resources.requests.AWSRequest;
import org.graylog.integrations.aws.resources.requests.AWSRequestImpl;
import org.graylog.integrations.aws.resources.responses.LogGroupsResponse;
import org.graylog.integrations.aws.service.CloudWatchService;
import org.graylog2.security.encryption.EncryptedValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClientBuilder;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.LogGroup;
import software.amazon.awssdk.services.cloudwatchlogs.paginators.DescribeLogGroupsIterable;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class CloudWatchServiceTest {

    @Mock
    private CloudWatchLogsClient cloudWatchLogsClient;
    @Mock
    private DescribeLogGroupsIterable logGroupsIterable;
    @Mock
    private AWSClientBuilderUtil awsClientBuilderUtil;
    @Mock
    private EncryptedValue encryptedValue;

    private CloudWatchService cloudWatchService;

    @BeforeEach
    public void setUp() {
        cloudWatchService = new CloudWatchService(mock(CloudWatchLogsClientBuilder.class), awsClientBuilderUtil);
    }

    @Test
    public void testLogGroupNames() {
        when(awsClientBuilderUtil.buildClient(any(CloudWatchLogsClientBuilder.class), any())).thenReturn(cloudWatchLogsClient);

        // Create a fake response that contains three log groups.
        DescribeLogGroupsResponse fakeLogGroupResponse = DescribeLogGroupsResponse
                .builder()
                .logGroups(LogGroup.builder().logGroupName("group-1").build(),
                        LogGroup.builder().logGroupName("group-2").build(),
                        LogGroup.builder().logGroupName("group-3").build())
                .build();

        // Mock out the response. When CloudWatchLogsClient.describeLogGroupsPaginator() is called,
        // return two responses with six messages total.
        List<DescribeLogGroupsResponse> responses = Arrays.asList(fakeLogGroupResponse, fakeLogGroupResponse);
        when(logGroupsIterable.iterator()).thenReturn(responses.iterator());
        when(cloudWatchLogsClient.describeLogGroupsPaginator(isA(DescribeLogGroupsRequest.class))).thenReturn(logGroupsIterable);

        final AWSRequest awsRequest = AWSRequestImpl.builder()
                .region(Region.US_EAST_1.id())
                .awsAccessKeyId("a-key")
                .awsSecretAccessKey(encryptedValue)
                .build();
        final LogGroupsResponse logGroupsResponse = cloudWatchService.getLogGroupNames(awsRequest);

        // Inspect the log groups returned and verify the contents and size.
        assertEquals(6, logGroupsResponse.total(), "The number of groups should be because the two responses " +
                "with 3 groups each were provided.");

        // Loop example to verify presence of a specific log group.
        boolean foundGroup = false;
        for (String logGroupName : logGroupsResponse.logGroups()) {
            if (logGroupName.equals("group-1")) {
                foundGroup = true;
                break;
            }
        }
        assertTrue(foundGroup);

        // One line check with stream.
        assertTrue(logGroupsResponse.logGroups().stream().anyMatch(logGroupName -> logGroupName.equals("group-2")));
    }
}
