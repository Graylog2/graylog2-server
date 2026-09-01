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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AWSAuthorizationFailureDetectorTest {

    private static final String QUERY = "Query";
    private static final String GET_RECORDS = "GetRecords";
    private static final String UPDATE_ITEM = "UpdateItem";

    /**
     * KCL schedules lease discovery - the call denied in the case this class exists for - at
     * {@code leaseAssignmentIntervalMillis / 2 - 25}, which is 9975ms with KCL's defaults.
     */
    private static final Duration LEASE_DISCOVERY_INTERVAL = Duration.ofMillis(9975);

    /**
     * Spacing wide enough that a handful of denials crosses the two-minute threshold, and still short enough not to
     * break the streak.
     */
    private static final Duration WIDE_SPACING = Duration.ofSeconds(20);

    private final List<Throwable> reportedFailures = new ArrayList<>();
    private final AtomicLong clock = new AtomicLong();

    private final AWSAuthorizationFailureDetector detector =
            new AWSAuthorizationFailureDetector(reportedFailures::add, clock::get);

    @Test
    void reportsOnceDenialsSpanTwoMinutes() {
        final DynamoDbException denial = accessDenied(
                "User: arn:aws:sts::123456789012:assumed-role/graylog/x is not authorized to perform: "
                        + "dynamodb:Query on resource: arn:aws:dynamodb:eu-west-1:123456789012:"
                        + "table/graylog-aws-plugin-test/index/LeaseOwnerToLeaseKeyIndex");

        // 13 denials at the real cadence span 119.7s - just inside the floor.
        for (int i = 0; i < 13; i++) {
            recordFailure(QUERY, denial);
            assertThat(reportedFailures).isEmpty();
            advance(LEASE_DISCOVERY_INTERVAL);
        }

        recordFailure(QUERY, denial);

        assertThat(reportedFailures).containsExactly(denial);
    }

    /**
     * The property that makes the fix work at all: KCL drives many operations through one client, so a counter shared
     * across operations is cleared by healthy traffic and never reaches any threshold.
     */
    @Test
    void successOnAnotherOperationDoesNotClearTheStreak() {
        final DynamoDbException denial = accessDenied("denied");

        for (int i = 0; i < 14; i++) {
            recordFailure(QUERY, denial);
            // A permitted lease renewal succeeds roughly three times per denied discovery attempt.
            recordSuccess(UPDATE_ITEM);
            recordSuccess(UPDATE_ITEM);
            recordSuccess(UPDATE_ITEM);
            advance(LEASE_DISCOVERY_INTERVAL);
        }

        assertThat(reportedFailures).containsExactly(denial);
    }

    @Test
    void successOnTheSameOperationClearsTheStreak() {
        denyRepeatedly(QUERY, 13, LEASE_DISCOVERY_INTERVAL);

        recordSuccess(QUERY);
        denyRepeatedly(QUERY, 13, LEASE_DISCOVERY_INTERVAL);

        assertThat(reportedFailures).isEmpty();
    }

    /**
     * KCL absorbs denials of these and keeps delivering records from the leases it already holds - a stalled metadata
     * migration, a lease-rebalancing scan, a table description it needs only for scan sizing, worker metrics. Stopping
     * an input that is still ingesting would be a worse bug than the log spam this class exists to stop, so only the
     * operations the consumer cannot work without are reported. Twenty minutes of continuous denial here changes
     * nothing.
     */
    @ParameterizedTest
    @ValueSource(strings = {"Scan", "UpdateItem"})
    void neverReportsAnOperationKclAbsorbs(String operation) {
        denyRepeatedly(operation, 60, WIDE_SPACING);

        assertThat(reportedFailures).isEmpty();
    }

    /**
     * The Kinesis half of the essential set. {@code PrefetchRecordsPublisher} swallows every SDK exception and
     * re-polls every 1.5s, so a denied fetch never fails a task and would otherwise leave the input reporting RUNNING
     * while consuming nothing.
     */
    @Test
    void reportsADeniedRecordFetch() {
        denyRepeatedly(GET_RECORDS, 100, Duration.ofMillis(1500));

        assertThat(reportedFailures).isNotEmpty();
    }

    /**
     * An operation issued every 1.5s must not fail an input over a short-lived denial, such as the seconds of
     * {@code AccessDeniedException} that IAM's eventual consistency can produce after a policy edit.
     */
    @Test
    void doesNotReportDenialsThatStopInsideTwoMinutes() {
        denyRepeatedly(QUERY, 60, Duration.ofMillis(1500));

        assertThat(reportedFailures).isEmpty();
    }

    @Test
    void doesNotReportASingleDenial() {
        recordFailure(QUERY, accessDenied("denied"));

        assertThat(reportedFailures).isEmpty();
    }

    @Test
    void doesNotReportJustShortOfTheThreshold() {
        final DynamoDbException denial = accessDenied("denied");

        recordFailure(QUERY, denial);
        advance(Duration.ofMinutes(2).minusNanos(1));
        recordFailure(QUERY, denial);

        assertThat(reportedFailures).isEmpty();
    }

    @Test
    void reportsExactlyAtTheThreshold() {
        final DynamoDbException denial = accessDenied("denied");

        recordFailure(QUERY, denial);
        advance(Duration.ofMinutes(2));
        recordFailure(QUERY, denial);

        assertThat(reportedFailures).containsExactly(denial);
    }

    /**
     * The streak-break gap is deliberately wider than the reporting threshold. Were they equal, an operation retried
     * at just over the threshold would restart its streak on every attempt and could never be reported.
     */
    @Test
    void reportsAnOperationRetriedMoreSlowlyThanTheThreshold() {
        final DynamoDbException denial = accessDenied("denied");

        recordFailure(QUERY, denial);
        advance(Duration.ofMinutes(3));
        recordFailure(QUERY, denial);

        assertThat(reportedFailures).containsExactly(denial);
    }

    /**
     * Without this, denials that are genuinely occasional would accumulate over days into a failure.
     */
    @Test
    void restartsTheStreakAfterALongGap() {
        denyRepeatedly(QUERY, 5, WIDE_SPACING);

        advance(Duration.ofMinutes(5));
        denyRepeatedly(QUERY, 5, WIDE_SPACING);

        assertThat(reportedFailures).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "AccessDeniedException",
            "AccessDenied",
            "UnrecognizedClientException",
            "InvalidClientTokenId",
            "InvalidSignatureException",
            "SignatureDoesNotMatch",
            "KMSAccessDeniedException",
            "KMSNotFoundException",
            "KMSOptInRequired",
            "KMSDisabledException"})
    void treatsDeniedActionsUnusableCredentialsAndUnusableKeysAsTerminal(String errorCode) {
        for (int i = 0; i < 10; i++) {
            recordFailure(QUERY, withErrorCode(errorCode, "denied"));
            advance(WIDE_SPACING);
        }

        assertThat(reportedFailures).isNotEmpty();
    }

    /**
     * Expired session credentials and throttling arrive as authorization-shaped errors but recover on their own.
     * Failing an input over either would be a worse bug than the log spam this class exists to stop.
     * {@code KMSInvalidStateException} is here because its service documentation does not say which key states
     * produce it, so an allowlist has to fail safe.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "ExpiredTokenException",
            "ExpiredToken",
            "ProvisionedThroughputExceededException",
            "ThrottlingException",
            "RequestLimitExceeded",
            "KMSInvalidStateException"})
    void neverReportsSelfHealingErrors(String errorCode) {
        for (int i = 0; i < 10; i++) {
            recordFailure(QUERY, withErrorCode(errorCode, "retry later"));
            advance(WIDE_SPACING);
        }

        assertThat(reportedFailures).isEmpty();
    }

    /**
     * Matching has to be exact: the allowlist carries the short code {@code AccessDenied}, which is a substring of
     * unrelated error codes, so a prefix or substring test would treat those as terminal.
     */
    @Test
    void matchesErrorCodesExactly() {
        for (int i = 0; i < 10; i++) {
            recordFailure(QUERY, withErrorCode("AccessDeniedExceptionFoo", "denied"));
            recordFailure(QUERY, withErrorCode("PreAccessDenied", "denied"));
            advance(WIDE_SPACING);
        }

        assertThat(reportedFailures).isEmpty();
    }

    @Test
    void detectsDenialsWrappedInACauseChain() {
        final DynamoDbException denial = accessDenied("denied");

        for (int i = 0; i < 10; i++) {
            recordFailure(QUERY, new CompletionException(denial));
            advance(WIDE_SPACING);
        }

        assertThat(reportedFailures).contains(denial);
    }

    @Test
    void ignoresFailuresThatAreNotAwsServiceExceptions() {
        for (int i = 0; i < 10; i++) {
            recordFailure(QUERY, new IOException("connection reset"));
            advance(WIDE_SPACING);
        }

        assertThat(reportedFailures).isEmpty();
    }

    @Test
    void ignoresAwsExceptionsWithoutErrorDetails() {
        for (int i = 0; i < 10; i++) {
            recordFailure(QUERY, AwsServiceException.builder().message("no details").build());
            advance(WIDE_SPACING);
        }

        assertThat(reportedFailures).isEmpty();
    }

    @Test
    void ignoresAwsExceptionsWithoutAnErrorCode() {
        // The JSON error unmarshaller leaves the code null when it cannot parse one (5xx/HTML/proxy body). The
        // terminal sets reject null, so a missing guard would NPE here rather than skip the exception.
        final AwsServiceException noCode = AwsServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorMessage("no code").build())
                .message("no code")
                .build();
        for (int i = 0; i < 10; i++) {
            recordFailure(QUERY, noCode);
            advance(WIDE_SPACING);
        }

        assertThat(reportedFailures).isEmpty();
    }

    /**
     * Fail closed. Unnamed calls sharing one bucket would let a success clear an unrelated denial, which is exactly
     * what per-operation tracking exists to prevent.
     */
    @Test
    void ignoresCallsWithNoOperationName() {
        // Deliberately not stubbing exception(): failing closed means returning before the throwable is even
        // inspected, and strict stubbing holds us to that.
        final Context.FailedExecution failedExecution = mock(Context.FailedExecution.class);

        for (int i = 0; i < 10; i++) {
            detector.onExecutionFailure(failedExecution, new ExecutionAttributes());
            advance(WIDE_SPACING);
        }

        assertThat(reportedFailures).isEmpty();
    }

    @Test
    void distinguishesRejectedCredentialsFromAMissingPermission() {
        assertThat(AWSAuthorizationFailureDetector.indicatesRejectedCredentials(
                withErrorCode("SignatureDoesNotMatch", "bad key"))).isTrue();
        assertThat(AWSAuthorizationFailureDetector.indicatesRejectedCredentials(
                new CompletionException(withErrorCode("UnrecognizedClientException", "unknown key")))).isTrue();
        assertThat(AWSAuthorizationFailureDetector.indicatesRejectedCredentials(
                accessDenied("not authorized to perform: dynamodb:Query"))).isFalse();
        assertThat(AWSAuthorizationFailureDetector.indicatesRejectedCredentials(
                new IOException("connection reset"))).isFalse();
    }

    private void denyRepeatedly(String operation, int times, Duration spacing) {
        for (int i = 0; i < times; i++) {
            recordFailure(operation, accessDenied("denied"));
            advance(spacing);
        }
    }

    /**
     * Drives the detector through the same {@link ExecutionInterceptor} hooks the SDK calls in production, so the tests
     * exercise the wiring rather than any test-only entry point.
     */
    private void recordFailure(String operation, Throwable throwable) {
        final Context.FailedExecution failedExecution = mock(Context.FailedExecution.class);
        when(failedExecution.exception()).thenReturn(throwable);
        detector.onExecutionFailure(failedExecution, attributesFor(operation));
    }

    private void recordSuccess(String operation) {
        detector.afterExecution(mock(Context.AfterExecution.class), attributesFor(operation));
    }

    private void advance(Duration duration) {
        clock.addAndGet(duration.toNanos());
    }

    private static ExecutionAttributes attributesFor(String operation) {
        return new ExecutionAttributes().putAttribute(SdkExecutionAttribute.OPERATION_NAME, operation);
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
