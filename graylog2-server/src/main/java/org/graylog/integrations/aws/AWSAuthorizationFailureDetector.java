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

import com.google.common.annotations.VisibleForTesting;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Watches an AWS client for authorization denials that no amount of retrying will fix, and reports one when a single
 * operation has been denied for {@link #MIN_DENIAL_PERIOD} <em>and</em> the consumer has made no progress in that
 * time.
 *
 * <p>The Kinesis Client Library reschedules its own calls forever and only logs the failure, so a missing IAM
 * permission produces an endless ERROR loop while the input consumes nothing. KCL exposes no hook for those failures,
 * but we build the AWS clients it uses, so an interceptor on them sees every denial the service returns.
 *
 * <p>Two conditions, because a denial alone does not mean the input is broken. KCL absorbs several denials and keeps
 * ingesting - a stalled metadata migration, a lease-rebalancing scan, a table description it only needs for scan
 * sizing - so reporting on the denial alone would stop inputs that are working. Requiring the consumer to have gone
 * quiet as well distinguishes "denied and dead" from "denied and degraded".
 *
 * <p>Denials are tracked per operation, not per client: one client carries many schedules at very different rates, so
 * state shared across operations would be cleared by healthy traffic before any threshold was reached. Note this keys
 * on the operation <em>name</em> while IAM authorizes on (action, resource), so a permitted call to a different table
 * under the same operation name does clear the streak.
 *
 * <p>The threshold is a duration rather than a number of attempts because cadences on one client differ by more than
 * an order of magnitude, so a fixed count would mean seconds for one operation and many minutes for another.
 *
 * <p>Use one instance per client. Operation names are not unique across services, so a shared map could conflate two
 * services' calls.
 */
public class AWSAuthorizationFailureDetector implements ExecutionInterceptor {

    private static final Duration MIN_DENIAL_PERIOD = Duration.ofMinutes(2);

    /**
     * Deliberately longer than {@link #MIN_DENIAL_PERIOD}. If the two were equal, an operation retried at just over
     * the threshold would restart its streak on every attempt and could never be reported at all.
     */
    private static final Duration STREAK_RESET_GAP = MIN_DENIAL_PERIOD.multipliedBy(2);

    private static final int MAX_CAUSE_DEPTH = 10;

    /**
     * Credentials that AWS rejects outright. Terminal for the same reason a denied action is, but the remediation is
     * different, so {@link #indicatesRejectedCredentials} lets the caller say so.
     */
    private static final Set<String> CREDENTIAL_ERROR_CODES = Set.of(
            "UnrecognizedClientException",
            "InvalidClientTokenId",
            "InvalidSignatureException",
            "SignatureDoesNotMatch");

    /**
     * Deliberately an allowlist. Expired session credentials and throttling also arrive as authorization-shaped
     * errors but recover on their own, and failing an input over a credential rotation would be worse than the log
     * spam this class exists to stop. The signature codes are included because a signature error that survives the
     * SDK's own retry-and-clock-adjust means a bad key, not skew.
     */
    private static final Set<String> TERMINAL_ERROR_CODES = Set.of(
            "AccessDeniedException",
            "AccessDenied",
            "UnrecognizedClientException",
            "InvalidClientTokenId",
            "InvalidSignatureException",
            "SignatureDoesNotMatch",
            // Unrecoverable KMS failures on an encrypted stream. KMSDisabledException and KMSInvalidStateException
            // are deliberately absent: those do recover on their own.
            "KMSAccessDeniedException",
            "KMSNotFoundException",
            "KMSOptInRequired");

    private final Consumer<Throwable> onTerminalFailure;
    private final LongSupplier lastProgressNanos;
    private final LongSupplier nanoClock;
    private final Map<String, DenialStreak> denialsByOperation = new ConcurrentHashMap<>();

    /**
     * @param onTerminalFailure called when a denial is judged unrecoverable. May be called more than once; the caller
     *                          is responsible for acting at most once.
     * @param lastProgressNanos timestamp of the last sign that the consumer is still doing useful work.
     * @param nanoClock         monotonic clock. Must be the same clock {@code lastProgressNanos} is stamped from, or
     *                          the two are not comparable.
     */
    public AWSAuthorizationFailureDetector(Consumer<Throwable> onTerminalFailure,
                                           LongSupplier lastProgressNanos,
                                           LongSupplier nanoClock) {
        this.onTerminalFailure = onTerminalFailure;
        this.lastProgressNanos = lastProgressNanos;
        this.nanoClock = nanoClock;
    }

    /**
     * Whether a denial means AWS rejected the credentials rather than that a permission is absent. The remediation
     * differs, so the two must not share a failure message.
     */
    public static boolean indicatesRejectedCredentials(Throwable throwable) {
        return denialWithCodeIn(throwable, CREDENTIAL_ERROR_CODES) != null;
    }

    @Override
    public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        final String operation = executionAttributes.getAttribute(SdkExecutionAttribute.OPERATION_NAME);
        if (operation == null) {
            // Fail closed. Unnamed calls sharing one bucket would let a success clear an unrelated denial, which is
            // exactly what per-operation tracking exists to prevent. The SDK always names generated calls.
            return;
        }
        recordFailure(operation, context.exception());
    }

    @Override
    public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
        final String operation = executionAttributes.getAttribute(SdkExecutionAttribute.OPERATION_NAME);
        if (operation != null) {
            recordSuccess(operation);
        }
    }

    @VisibleForTesting
    void recordFailure(String operation, Throwable throwable) {
        final AwsServiceException denial = denialWithCodeIn(throwable, TERMINAL_ERROR_CODES);
        if (denial == null) {
            return;
        }
        final long now = nanoClock.getAsLong();
        final DenialStreak streak = denialsByOperation.compute(operation,
                (ignored, current) -> current == null ? DenialStreak.first(now) : current.next(now));
        if (streak.isTerminal() && madeNoProgress(now)) {
            onTerminalFailure.accept(denial);
        }
    }

    @VisibleForTesting
    void recordSuccess(String operation) {
        denialsByOperation.remove(operation);
    }

    /**
     * Whether the consumer has gone quiet for as long as the denial has persisted. KCL keeps delivering records from
     * the leases it already holds even while a coordination call is denied, and an input that is still ingesting must
     * not be stopped.
     */
    private boolean madeNoProgress(long nowNanos) {
        return nowNanos - lastProgressNanos.getAsLong() >= MIN_DENIAL_PERIOD.toNanos();
    }

    /**
     * The reported throwable is defensively unwrapped: the async SDK strips {@code CompletionException} before
     * interceptors run, so in practice the denial arrives directly, but nothing guarantees that for every wrapper.
     */
    @Nullable
    private static AwsServiceException denialWithCodeIn(Throwable throwable, Set<String> errorCodes) {
        Throwable current = throwable;
        for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (current instanceof AwsServiceException awsException
                    && awsException.awsErrorDetails() != null
                    && errorCodes.contains(awsException.awsErrorDetails().errorCode())) {
                return awsException;
            }
            current = current.getCause();
        }
        return null;
    }

    /**
     * An unbroken run of denials of one operation. A gap longer than {@link #STREAK_RESET_GAP} starts a new run, so
     * denials that are genuinely occasional cannot accumulate into a failure over days.
     */
    private record DenialStreak(long firstNanos, long lastNanos) {

        private static DenialStreak first(long nowNanos) {
            return new DenialStreak(nowNanos, nowNanos);
        }

        private DenialStreak next(long nowNanos) {
            if (nowNanos - lastNanos > STREAK_RESET_GAP.toNanos()) {
                return first(nowNanos);
            }
            return new DenialStreak(firstNanos, nowNanos);
        }

        private boolean isTerminal() {
            return lastNanos - firstNanos >= MIN_DENIAL_PERIOD.toNanos();
        }
    }
}
