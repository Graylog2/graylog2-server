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
import org.graylog.integrations.aws.AWSClientBuilderUtil;
import org.graylog.integrations.aws.AWSMessageType;
import org.graylog.integrations.aws.resources.requests.AWSRequest;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.system.SimpleNodeId;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class KinesisConsumerTest {

    private static final String STREAM = "test-stream";

    private final InputFailureRecorder inputFailureRecorder = mock(InputFailureRecorder.class);

    @Test
    void authorizationFailureFailsTheInputAndStopsTheConsumer() {
        final DynamoDbException denial = accessDeniedException();
        final KinesisConsumer consumer = spy(newConsumer());

        consumer.handleAuthorizationFailure(denial);

        verify(inputFailureRecorder).setFailing(eq(KinesisConsumer.class), contains(STREAM), eq(denial));
        // The callback arrives on an SDK event loop thread and Scheduler.shutdown() blocks, so the stop has to
        // be handed off to another thread.
        verify(consumer, timeout(5_000)).stop();
    }

    /**
     * KCL keeps processing already-owned leases while it shuts down, and every successful PROCESS task reports
     * the input healthy again. That must not undo a terminal authorization failure, or the input ends up
     * showing RUNNING with no consumer behind it.
     */
    @Test
    void successfulTaskDoesNotClearATerminalAuthorizationFailure() {
        final KinesisConsumer consumer = newConsumer();

        consumer.handleAuthorizationFailure(accessDeniedException());
        consumer.recordTaskSuccess();

        verify(inputFailureRecorder, never()).setRunning();
    }

    @Test
    void successfulTaskClearsAnOrdinaryFailure() {
        final KinesisConsumer consumer = newConsumer();

        consumer.recordTaskSuccess();

        verify(inputFailureRecorder).setRunning();
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
                mock(AWSClientBuilderUtil.class),
                inputFailureRecorder,
                false);
    }
}
