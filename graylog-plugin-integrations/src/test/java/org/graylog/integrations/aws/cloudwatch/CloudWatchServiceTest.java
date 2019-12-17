package org.graylog.integrations.aws.cloudwatch;

import org.graylog.integrations.aws.resources.requests.AWSRequest;
import org.graylog.integrations.aws.resources.requests.AWSRequestImpl;
import org.graylog.integrations.aws.resources.responses.LogGroupsResponse;
import org.graylog.integrations.aws.service.CloudWatchService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClientBuilder;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.LogGroup;
import software.amazon.awssdk.services.cloudwatchlogs.paginators.DescribeLogGroupsIterable;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

public class CloudWatchServiceTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private CloudWatchLogsClientBuilder logsClientBuilder;

    @Mock
    private CloudWatchLogsClient cloudWatchLogsClient;

    @Mock
    private DescribeLogGroupsIterable logGroupsIterable;

    private CloudWatchService cloudWatchService;

    @Before
    public void setUp() {

        cloudWatchService = new CloudWatchService(logsClientBuilder);
    }

    @Test
    public void testLogGroupNames() {

        // Perform test setup. Return the builder and client when appropriate.
        when(logsClientBuilder.region(isA(Region.class))).thenReturn(logsClientBuilder);
        when(logsClientBuilder.credentialsProvider(isA(AwsCredentialsProvider.class))).thenReturn(logsClientBuilder);
        when(logsClientBuilder.build()).thenReturn(cloudWatchLogsClient);

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
                                                    .awsSecretAccessKey("a-secret")
                                                    .build();
        final LogGroupsResponse logGroupsResponse = cloudWatchService.getLogGroupNames(awsRequest);

        // Inspect the log groups returned and verify the contents and size.
        assertEquals("The number of groups should be because the two responses " +
                     "with 3 groups each were provided.", 6, logGroupsResponse.total());

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