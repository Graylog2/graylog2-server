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

import org.glassfish.jersey.internal.util.Producer;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public interface GraylogRestApi {
    int TIMEOUT_MS = 10000;
    int SLEEP_MS = 500;

    default void waitFor(Producer<Boolean> predicate, String timeoutErrorMessage) {
        waitForObject(() -> predicate.call() ? Optional.of(true) : Optional.empty(), timeoutErrorMessage);
    }

     default <T> T waitForObject(Producer<Optional<T>> predicate, String timeoutErrorMessage) {
        return waitForObject(predicate, timeoutErrorMessage, Duration.of(TIMEOUT_MS, ChronoUnit.MILLIS));
    }

     default <T> T waitForObject(Producer<Optional<T>> predicate, String timeoutErrorMessage, Duration timeout) {
        int msPassed = 0;
        while (msPassed <= timeout.toMillis()) {
            final Optional<T> result = predicate.call();
            if (result != null && result.isPresent()) {
                return result.get();
            }
            msPassed += SLEEP_MS;
            sleep();
        }
        throw new AssertionError(timeoutErrorMessage);
    }

    private void sleep() {
        try {
            Thread.sleep(SLEEP_MS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
