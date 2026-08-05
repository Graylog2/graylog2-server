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
package org.graylog.integrations.aws;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;

class AWSAuthorizationFailureDetectorTest {

    private final List<Throwable> reportedFailures = new ArrayList<>();
    private final AWSAuthorizationFailureDetector detector =
            new AWSAuthorizationFailureDetector(reportedFailures::add);

    @Test
    void reportsAfterThreeConsecutiveAuthorizationDenials() {
        final DynamoDbException denial = accessDenied(
                "User: arn:aws:sts::123456789012:assumed-role/graylog/x is not authorized to perform: "
                        + "dynamodb:Query on resource: arn:aws:dynamodb:eu-west-1:123456789012:"
                        + "table/graylog-aws-plugin-test/index/LeaseOwnerToLeaseKeyIndex");

        detector.recordFailure(denial);
        detector.recordFailure(denial);
        assertThat(reportedFailures).isEmpty();

        detector.recordFailure(denial);

        assertThat(reportedFailures).containsExactly(denial);
    }

    @Test
    void resetsTheCountWhenACallSucceedsInBetween() {
        detector.recordFailure(accessDenied("denied"));
        detector.recordFailure(accessDenied("denied"));
        detector.recordSuccess();
        detector.recordFailure(accessDenied("denied"));
        detector.recordFailure(accessDenied("denied"));

        assertThat(reportedFailures).isEmpty();
    }

    @Test
    void reportsAtMostOnce() {
        for (int i = 0; i < 10; i++) {
            detector.recordFailure(accessDenied("denied"));
        }

        assertThat(reportedFailures).hasSize(1);
    }

    @Test
    void detectsDenialsWrappedByTheAsyncClient() {
        final DynamoDbException denial = accessDenied("denied");

        for (int i = 0; i < 3; i++) {
            detector.recordFailure(new CompletionException(denial));
        }

        assertThat(reportedFailures).containsExactly(denial);
    }

    @Test
    void treatsUnusableCredentialsAsTerminal() {
        for (int i = 0; i < 3; i++) {
            detector.recordFailure(withErrorCode("UnrecognizedClientException", "token is invalid"));
        }

        assertThat(reportedFailures).hasSize(1);
    }

    /**
     * Expired session credentials also surface as an authorization error, but
     * {@code StsAssumeRoleCredentialsProvider} refreshes them on its own. Failing the input over a credential
     * rotation would be a worse bug than the log spam this class exists to stop.
     */
    @Test
    void neverReportsExpiredCredentials() {
        for (int i = 0; i < 10; i++) {
            detector.recordFailure(withErrorCode("ExpiredTokenException", "token expired"));
        }

        assertThat(reportedFailures).isEmpty();
    }

    @Test
    void neverReportsThrottling() {
        for (int i = 0; i < 10; i++) {
            detector.recordFailure(withErrorCode("ProvisionedThroughputExceededException", "slow down"));
        }

        assertThat(reportedFailures).isEmpty();
    }

    @Test
    void ignoresFailuresThatAreNotAwsServiceExceptions() {
        for (int i = 0; i < 10; i++) {
            detector.recordFailure(new IOException("connection reset"));
        }

        assertThat(reportedFailures).isEmpty();
    }

    private static DynamoDbException accessDenied(String message) {
        // DynamoDB answers IAM denials with HTTP 400 and the error code in the payload, so classification
        // must key on the error code rather than the status code.
        return (DynamoDbException) DynamoDbException.builder()
                .statusCode(400)
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode("AccessDeniedException")
                        .errorMessage(message)
                        .build())
                .message(message)
                .build();
    }

    private static AwsServiceException withErrorCode(String errorCode, String message) {
        return AwsServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode(errorCode)
                        .errorMessage(message)
                        .build())
                .message(message)
                .build();
    }
}
