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
package org.graylog.integrations.aws.transports;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.integrations.aws.AWSAuthorizationFailureDetector;
import org.graylog.integrations.aws.AWSClientBuilderUtil;
import org.graylog.integrations.aws.AWSMessageType;
import org.graylog.integrations.aws.resources.requests.AWSRequest;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.system.SimpleNodeId;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.client.builder.SdkClientBuilder;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KinesisConsumerTest {

    private static final String STREAM = "test-stream";

    private final InputFailureRecorder inputFailureRecorder = mock(InputFailureRecorder.class);
    private final AWSClientBuilderUtil awsClientBuilderUtil = mock(AWSClientBuilderUtil.class);
    private final AtomicLong clock = new AtomicLong();

    /**
     * The load-bearing test for the whole feature: it drives a denial through the interceptor actually installed on
     * the DynamoDB client, so it fails if the detector is not wired to {@code handleAuthorizationFailure} at all.
     * Asserting that the interceptor merely exists leaves that possible.
     */
    @Test
    void aDenialSeenByTheInstalledInterceptorFailsTheInput() {
        final ExecutionInterceptor detector = detectorsOn(newClientBuilders().dynamoDb()).get(0);
        final DynamoDbException denial = accessDeniedException();
        final Context.FailedExecution failedExecution = mock(Context.FailedExecution.class);
        when(failedExecution.exception()).thenReturn(denial);

        detector.onExecutionFailure(failedExecution, attributesFor("Query"));
        clock.addAndGet(Duration.ofMinutes(3).toNanos());
        detector.onExecutionFailure(failedExecution, attributesFor("Query"));

        verify(inputFailureRecorder).setTerminallyFailing(eq(KinesisConsumer.class), contains(STREAM), eq(denial));
    }

    @Test
    void authorizationFailureFailsTheInputAndStopsTheConsumerOffTheCallbackThread() throws Exception {
        final DynamoDbException denial = accessDeniedException();
        final KinesisConsumer consumer = spy(newConsumer());
        final AtomicReference<String> stopThreadName = new AtomicReference<>();
        final CountDownLatch stopCalled = new CountDownLatch(1);
        doAnswer(invocation -> {
            stopThreadName.set(Thread.currentThread().getName());
            stopCalled.countDown();
            return null;
        }).when(consumer).stop();

        consumer.handleAuthorizationFailure(denial);

        // Terminal, so it has to replace any transient failure already recorded rather than be dropped.
        verify(inputFailureRecorder).setTerminallyFailing(eq(KinesisConsumer.class), contains(STREAM), eq(denial));
        assertThat(stopCalled.await(5, TimeUnit.SECONDS)).isTrue();
        // stop() blocks for up to 20s on a shared SDK response-completion thread, so it must not run inline. The name
        // carries the stream so that concurrent failures are distinguishable in a thread dump.
        assertThat(stopThreadName.get()).isEqualTo(KinesisConsumer.shutdownThreadName(STREAM));
    }

    // Several tests here let the real stop() run on the shutdown thread. That is safe and immediate: the scheduler
    // was never started, so stop() returns without touching it.

    @Test
    void handlesOnlyTheFirstAuthorizationFailure() {
        final KinesisConsumer consumer = newConsumer();

        consumer.handleAuthorizationFailure(accessDeniedException());
        consumer.handleAuthorizationFailure(accessDeniedException());

        verify(inputFailureRecorder, times(1)).setTerminallyFailing(any(), anyString(), any());
    }

    /**
     * Granting a permission and correcting a rejected key are different actions, so the message must not tell the
     * operator to grant something when AWS refused the credentials outright.
     */
    @Test
    void namesTheCredentialsRatherThanAPermissionWhenAwsRejectsTheKey() {
        final KinesisConsumer consumer = newConsumer();

        consumer.handleAuthorizationFailure(withErrorCode("SignatureDoesNotMatch"));

        verify(inputFailureRecorder).setTerminallyFailing(eq(KinesisConsumer.class), contains("credentials"), any());
    }

    /**
     * The exclusions are the load-bearing part: sharing one detector across clients lets operation names collide
     * between services, and watching CloudWatch would fail an input over lost metrics.
     */
    @Test
    void watchesDynamoDbAndKinesisForAuthorizationFailuresButNotCloudWatch() {
        final KinesisConsumer.ClientBuilders builders = newClientBuilders();

        assertThat(detectorsOn(builders.dynamoDb())).hasSize(1);
        assertThat(detectorsOn(builders.kinesis())).hasSize(1);
        assertThat(detectorsOn(builders.cloudWatch())).isEmpty();
    }

    @Test
    void givesEachWatchedClientItsOwnDetector() {
        final KinesisConsumer.ClientBuilders builders = newClientBuilders();

        assertThat(detectorsOn(builders.dynamoDb()).get(0))
                .isNotSameAs(detectorsOn(builders.kinesis()).get(0));
    }

    private static List<ExecutionInterceptor> detectorsOn(SdkClientBuilder<?, ?> builder) {
        return builder.overrideConfiguration().executionInterceptors().stream()
                .filter(AWSAuthorizationFailureDetector.class::isInstance)
                .toList();
    }

    private static ExecutionAttributes attributesFor(String operation) {
        return new ExecutionAttributes().putAttribute(SdkExecutionAttribute.OPERATION_NAME, operation);
    }

    private KinesisConsumer.ClientBuilders newClientBuilders() {
        return newConsumer().createClientBuilders(Region.EU_WEST_1, mock(AwsCredentialsProvider.class));
    }

    private static DynamoDbException accessDeniedException() {
        return (DynamoDbException) DynamoDbException.builder()
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode("AccessDeniedException")
                        .errorMessage("User: arn:aws:sts::1:assumed-role/graylog/x is not authorized to perform: "
                                + "dynamodb:Query on resource: arn:aws:dynamodb:eu-west-1:1:"
                                + "table/graylog-aws-plugin-test-stream/index/LeaseOwnerToLeaseKeyIndex")
                        .build())
                .message("not authorized")
                .build();
    }

    private static DynamoDbException withErrorCode(String errorCode) {
        return (DynamoDbException) DynamoDbException.builder()
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode(errorCode)
                        .errorMessage(errorCode)
                        .build())
                .message(errorCode)
                .build();
    }

    private KinesisConsumer newConsumer() {
        return new KinesisConsumer(
                new SimpleNodeId("00000000-0000-0000-0000-000000000000"),
                mock(KinesisTransport.class),
                new ObjectMapper(),
                rawMessage -> {},
                STREAM,
                AWSMessageType.KINESIS_RAW,
                10_000,
                mock(AWSRequest.class),
                awsClientBuilderUtil,
                inputFailureRecorder,
                clock::get);
    }
}
