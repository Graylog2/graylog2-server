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
package org.graylog.aws.inputs.cloudtrail;

import org.graylog.aws.inputs.cloudtrail.external.CloudTrailClientFactory;
import org.graylog.aws.sqs.SQSClientFactory;
import org.graylog.integrations.aws.AWSClientBuilderUtil;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MisfireException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.graylog.aws.inputs.cloudtrail.CloudTrailInput.CK_ASSUME_ROLE_ARN;
import static org.graylog.aws.inputs.cloudtrail.CloudTrailInput.CK_AWS_ACCESS_KEY;
import static org.graylog.aws.inputs.cloudtrail.CloudTrailInput.CK_AWS_SQS_REGION;
import static org.graylog.aws.inputs.cloudtrail.CloudTrailInput.CK_POLLING_INTERVAL;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CloudTrailTransportTest {
    // Code Under Test
    @InjectMocks
    CloudTrailTransport cut;

    // Mock Objects
    @Mock
    Configuration mockConfiguration;
    @Mock
    CloudTrailClientFactory mockCloudTrailClientFactory;
    @Mock
    SQSClientFactory sqsClientFactory;
    @Mock
    AWSClientBuilderUtil clientUtils;
    @Mock
    ScheduledExecutorService mockExecutorService;
    @Mock
    CloudTrailInput mockCloudTrailInput;
    @Mock
    ScheduledFuture mockRunningTask;
    @Mock
    InputFailureRecorder mockInputFailureRecorder;

    // Test objects
    private static final String TEST_USER_NAME = "username";
    private static final String TEST_REGION = "us-east-1";
    private static final String TEST_ARN = "test:arn:aws:iam::123456789012:role/test-role";

    // Test Cases
    @Test
    public void doLaunch_schedulesPollerTask_whenConfigurationIsValid() throws Exception {
        givenGoodConfiguration(5);
        givenGoodExecutorService();

        whenDoLaunchIsCalled();
        thenAwsClientBuilderUtilsCalled();
    }

    @Test
    public void doStop_terminatesPollerTask_whenTaskIsRunning() throws Exception {
        givenGoodConfiguration(1);
        givenGoodExecutorService();

        whenDoLaunchIsCalled();

        thenTaskSubmittedToExecutor(1L);

        whenDoStopIsCalled();

        thenRunningTaskIsCancelled();
    }

    @Test
    public void doStop_doesNothing_whenTaskIsNotRunning() {
        whenDoStopIsCalled();

        thenRunningTaskIsNotCancelled();
    }

    // GIVENs
    private void givenGoodConfiguration(int pollingInterval) {
        given(mockConfiguration.getString(eq(CK_AWS_ACCESS_KEY))).willReturn(TEST_USER_NAME);
        given(mockConfiguration.getString(eq(CK_ASSUME_ROLE_ARN))).willReturn(TEST_ARN);
        given(mockConfiguration.getString(eq(CK_AWS_SQS_REGION))).willReturn(TEST_REGION);

        given(mockConfiguration.getInt(eq(CK_POLLING_INTERVAL))).willReturn(pollingInterval);
        given(mockCloudTrailInput.getConfiguration()).willReturn(mockConfiguration);
    }

    private void givenGoodExecutorService() {
        given(mockExecutorService.scheduleWithFixedDelay(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class)))
                .willReturn(mockRunningTask);
        given(clientUtils.createCredentialsProvider(any())).willReturn(mock(AwsCredentialsProvider.class));
    }

    private void thenAwsClientBuilderUtilsCalled() {
        verify(clientUtils, times(1)).createCredentialsProvider(any());
    }

    // WHENs
    private void whenDoLaunchIsCalled() throws MisfireException {
        cut.doLaunch(mockCloudTrailInput, mockInputFailureRecorder);
    }

    private void whenDoStopIsCalled() {
        cut.doStop();
    }

    // THENs
    private void thenTaskSubmittedToExecutor(Long pollingInterval) {
        ArgumentCaptor<CloudTrailPollerTask> taskCaptor = ArgumentCaptor.forClass(CloudTrailPollerTask.class);
        ArgumentCaptor<Long> initialDelayCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> intervalCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<TimeUnit> timeUnitCaptor = ArgumentCaptor.forClass(TimeUnit.class);

        verify(sqsClientFactory, times(1)).create(any(), eq(TEST_REGION),
                isA(AwsCredentialsProvider.class), eq(mockInputFailureRecorder));
        verify(mockExecutorService, times(1)).scheduleWithFixedDelay(taskCaptor.capture(),
                initialDelayCaptor.capture(), intervalCaptor.capture(), timeUnitCaptor.capture());

        assertThat(initialDelayCaptor.getValue(), is(0L));
        assertThat(intervalCaptor.getValue(), is(pollingInterval));
        assertThat(timeUnitCaptor.getValue(), is(MINUTES));
    }

    private void thenRunningTaskIsCancelled() {
        ArgumentCaptor<Boolean> mayInterruptCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(mockRunningTask, times(1)).cancel(mayInterruptCaptor.capture());

        assertThat(mayInterruptCaptor.getValue(), is(false));
    }

    private void thenRunningTaskIsNotCancelled() {
        verify(mockRunningTask, times(0)).cancel(anyBoolean());
    }

}
