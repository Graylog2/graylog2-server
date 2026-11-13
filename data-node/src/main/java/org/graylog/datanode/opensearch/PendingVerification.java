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
package org.graylog.datanode.opensearch;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * One {@link CertificateReloadVerifier#verify} call's worth of state: the certificate serial it's waiting to see
 * served, when it was requested, the recurring check that's polling for it, and the future callers get back to
 * learn when it's done.
 */
final class PendingVerification {
    private final BigInteger expectedSerialNumber;
    private final Instant requestedAt;
    private final CompletableFuture<Void> completed = new CompletableFuture<>();
    private final ScheduledFuture<?> task;

    private PendingVerification(BigInteger expectedSerialNumber, Instant requestedAt, ScheduledExecutorService scheduler,
                                 Duration retryInterval, Consumer<PendingVerification> check) {
        this.expectedSerialNumber = expectedSerialNumber;
        this.requestedAt = requestedAt;
        this.task = scheduler.scheduleWithFixedDelay(() -> check.accept(this),
                retryInterval.toMillis(), retryInterval.toMillis(), TimeUnit.MILLISECONDS);
    }

    // creates a verification for expectedSerialNumber and starts check running on scheduler every retryInterval,
    // passing this same instance back on every run.
    static PendingVerification schedule(BigInteger expectedSerialNumber, ScheduledExecutorService scheduler,
                                         Duration retryInterval, Consumer<PendingVerification> check) {
        return new PendingVerification(expectedSerialNumber, Instant.now(), scheduler, retryInterval, check);
    }

    BigInteger expectedSerialNumber() {
        return expectedSerialNumber;
    }

    Instant requestedAt() {
        return requestedAt;
    }

    // the future callers get back from CertificateReloadVerifier.verify(); resolves once stop() runs.
    CompletableFuture<Void> completed() {
        return completed;
    }

    // safe to call from within the check that's currently running: cancel(false) only skips the *next* run.
    void stop() {
        task.cancel(false);
        completed.complete(null);
    }
}
