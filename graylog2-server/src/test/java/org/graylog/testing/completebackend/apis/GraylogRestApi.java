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
package org.graylog.testing.completebackend.apis;

import org.awaitility.Awaitility;
import org.glassfish.jersey.internal.util.Producer;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public interface GraylogRestApi {
    Duration DEFAULT_TIMEOUT = Duration.of(60, ChronoUnit.SECONDS);

    default void waitFor(Producer<Boolean> predicate, String timeoutErrorMessage) {
        waitForObject(() -> predicate.call() ? Optional.of(true) : Optional.empty(), timeoutErrorMessage);
    }

     default <T> T waitForObject(Producer<Optional<T>> predicate, String timeoutErrorMessage) {
        return waitForObject(predicate, timeoutErrorMessage, DEFAULT_TIMEOUT);
    }

     default <T> T waitForObject(Producer<Optional<T>> predicate, String timeoutErrorMessage, Duration timeout) {
         return Awaitility.waitAtMost(new org.awaitility.Duration(timeout.toMillis(), TimeUnit.MILLISECONDS))
                 .pollInterval(org.awaitility.Duration.FIVE_HUNDRED_MILLISECONDS)
                 .until(predicate, Optional::isPresent)
                 .orElseThrow(() -> new AssertionError(timeoutErrorMessage));
    }
}
