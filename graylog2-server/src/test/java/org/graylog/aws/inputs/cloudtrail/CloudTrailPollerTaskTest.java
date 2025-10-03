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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.aws.inputs.cloudtrail.external.CloudTrailS3Client;
import org.graylog.aws.notifications.SNSNotification;
import org.graylog.aws.notifications.SNSNotificationParser;
import org.graylog.aws.notifications.SQSClient;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTimeUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CloudTrailPollerTaskTest {

    private static final int BATCH_SIZE = 10;
    // Code Under Test
    private CloudTrailPollerTask cut;

    // Mock Objects
    @Mock
    private CloudTrailInput mockInput;
    @Mock
    private SQSClient sqsClient;
    @Mock
    private CloudTrailS3Client mockS3Client;

    @Mock
    private CloudTrailTransport mockTransport;
    @Mock
    private InputFailureRecorder inputFailureRecorder;

    @Before
    public void setUp() throws Exception {
        cut = new CloudTrailPollerTask(mockInput,
                sqsClient,
                mockS3Client,
                mockTransport,
                inputFailureRecorder,
                new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES),
                new AtomicBoolean(false));
    }

    @AfterClass
    public static void releaseFixedDate() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    // Test Cases
    @Test
    public void run_waitsForThrottleRelease_whenTransportIsThrottled() {
        givenTransportIsThrottled();

        whenRunIsCalled();

        thenBlockUntilThrottleWillBeCalled();
    }

    @Test
    public void run_doesNotWaitForThrottleRelease_whenTransportIsNotThrottled() {
        givenTransportIsNotThrottled();

        whenRunIsCalled();

        thenBlockUntilThrottleWillNotBeCalled();
    }

    @Test
    public void run_triggersInputFail_whenAllContentTypesFail() {
        givenProblemWithCloudTrailConnection();
        whenRunIsCalled();
        thenInputIsShutDown();
    }

    @Test
    public void run_fetchesAndPersistsLogs_whenContentIsAvailable() throws Exception {
        givenTransportIsNotThrottled();
        givenContentIsAvailable(BATCH_SIZE);

        whenRunIsCalled();

        thenBlockUntilThrottleWillNotBeCalled();
        thenContentQueryIsExecuted();
        thenRecordsPersistedToJournal(1);
        thenInputIsNotShutDown();
    }

    private void givenTransportIsThrottled() {
        given(mockTransport.isThrottled()).willReturn(true);
    }

    private void givenTransportIsNotThrottled() {
        given(mockTransport.isThrottled()).willReturn(false);
    }

    private void givenProblemWithCloudTrailConnection() {
        given(sqsClient.getNotifications(BATCH_SIZE)).willThrow(new RuntimeException());
    }

    private void givenContentIsAvailable(int recordCount) throws IOException {
        ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final Message message = Message.builder()
                .body("{\n" +
                        "  \"Type\" : \"Notification\",\n" +
                        "  \"MessageId\" : \"11a04c4a-094e-5395-b297-00eaefda2893\",\n" +
                        "  \"TopicArn\" : \"arn:aws:sns:eu-west-1:459220251735:cloudtrail-write\",\n" +
                        "  \"Message\" : \"{\\\"s3Bucket\\\":\\\"cloudtrailbucket\\\",\\\"s3ObjectKey\\\":[\\\"example/AWSLogs/123456789012/CloudTrail/us-east-1/2020/09/15/example.json.gz\\\"]}\",\n" +
                        "  \"Timestamp\" : \"2020-09-15T16:22:44.011Z\"\n" +
                        "}").build();

        SNSNotificationParser parser = new SNSNotificationParser(objectMapper);

        String cloudTrailLog = """
                {
                  "Records": [
                    {
                      "eventVersion": "1.05",
                      "userIdentity": {
                        "type": "IAMUser",
                        "principalId": "EX_PRINCIPAL_ID",
                        "arn": "arn:aws:iam::123456789012:user/Alice",
                        "accountId": "123456789012",
                        "accessKeyId": "EXAMPLE_KEY_ID",
                        "userName": "Alice"
                      },
                      "eventTime": "2020-09-15T12:00:00Z",
                      "eventSource": "ec2.amazonaws.com",
                      "eventName": "StartInstances",
                      "awsRegion": "us-east-1",
                      "sourceIPAddress": "205.251.233.176",
                      "userAgent": "ec2-api-tools 1.6.12.2",
                      "requestParameters": {
                        "instancesSet": {
                          "items": [
                            {
                              "instanceId": "i-1234567890abcdef0"
                            }
                          ]
                        }
                      },
                      "responseElements": {
                        "_return": true
                      }
                    }
                  ]
                }
                """;

        List<SNSNotification> notifications = parser.parse(message);
        given(mockS3Client.readCompressed(anyString(), anyString())).willReturn(cloudTrailLog);
        given(sqsClient.getNotifications(eq(BATCH_SIZE))).willReturn(notifications).willReturn(List.of());
    }

    // WHENS
    private void whenRunIsCalled() {
        cut.run();
    }

    // THENs
    private void thenBlockUntilThrottleWillBeCalled() {
        verify(mockTransport, times(1)).blockUntilUnthrottled();
    }

    private void thenBlockUntilThrottleWillNotBeCalled() {
        verify(mockTransport, times(0)).blockUntilUnthrottled();
    }

    private void thenContentQueryIsExecuted() {
        verify(sqsClient, times(2)).getNotifications(eq(BATCH_SIZE));
    }

    private void thenRecordsPersistedToJournal(int recordCount) {
        verify(mockInput, times(recordCount)).processRawMessage(any(RawMessage.class));
    }

    private void thenInputIsShutDown() {
        verify(inputFailureRecorder, atLeastOnce()).setFailing(any(), any(), any());
    }

    private void thenInputIsNotShutDown() {
        verify(inputFailureRecorder, times(1)).setRunning();
    }


}
