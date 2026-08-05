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

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Watches an AWS client for authorization denials that no amount of retrying will fix, and reports the first
 * one that repeats {@value #CONSECUTIVE_DENIAL_THRESHOLD} times in a row.
 *
 * <p>The Kinesis Client Library retries its own DynamoDB calls on a fixed schedule forever and only logs the
 * failure, so a missing IAM permission produces an endless ERROR loop while the input consumes nothing. Since
 * we build the AWS clients that KCL uses, attaching this interceptor to them is the only place we can observe
 * those failures.
 *
 * <p>Use one instance per client: a success on one service must not reset the denial count of another.
 */
public class AWSAuthorizationFailureDetector implements ExecutionInterceptor {

    private static final int CONSECUTIVE_DENIAL_THRESHOLD = 3;
    private static final int MAX_CAUSE_DEPTH = 10;

    /**
     * Deliberately an allowlist. Expired session credentials and throttling also arrive as authorization-shaped
     * errors but recover on their own, and failing an input over a credential rotation would be worse than the
     * log spam this class exists to stop.
     */
    private static final Set<String> TERMINAL_ERROR_CODES = Set.of(
            "AccessDeniedException",
            "AccessDenied",
            "UnrecognizedClientException",
            "InvalidClientTokenId",
            "InvalidSignatureException",
            "SignatureDoesNotMatch");

    private final Consumer<Throwable> onTerminalFailure;
    private final AtomicInteger consecutiveDenials = new AtomicInteger();
    private final AtomicBoolean reported = new AtomicBoolean();

    public AWSAuthorizationFailureDetector(Consumer<Throwable> onTerminalFailure) {
        this.onTerminalFailure = onTerminalFailure;
    }

    @Override
    public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        recordFailure(context.exception());
    }

    @Override
    public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
        recordSuccess();
    }

    void recordFailure(Throwable throwable) {
        final AwsServiceException denial = terminalDenial(throwable);
        if (denial == null) {
            return;
        }
        if (consecutiveDenials.incrementAndGet() >= CONSECUTIVE_DENIAL_THRESHOLD
                && reported.compareAndSet(false, true)) {
            onTerminalFailure.accept(denial);
        }
    }

    void recordSuccess() {
        consecutiveDenials.set(0);
    }

    /**
     * The async clients hand exceptions back wrapped in {@link java.util.concurrent.CompletionException}, so the
     * whole cause chain has to be inspected.
     */
    @Nullable
    private static AwsServiceException terminalDenial(Throwable throwable) {
        Throwable current = throwable;
        for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (current instanceof AwsServiceException awsException
                    && awsException.awsErrorDetails() != null
                    && TERMINAL_ERROR_CODES.contains(awsException.awsErrorDetails().errorCode())) {
                return awsException;
            }
            current = current.getCause() == current ? null : current.getCause();
        }
        return null;
    }
}
