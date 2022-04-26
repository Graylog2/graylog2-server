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
package org.graylog.testing.utils;

import org.glassfish.jersey.internal.util.Producer;

import java.util.Optional;

import static org.junit.Assert.fail;

public final class WaitUtils {

    private static final int TIMEOUT_MS = 10000;
    private static final int SLEEP_MS = 500;

    private WaitUtils() {
    }

    public static void waitFor(Producer<Boolean> predicate, String timeoutErrorMessage) {
        waitForObject(() -> predicate.call() ? Optional.of(true) : Optional.empty(), timeoutErrorMessage);
    }

    public static <T> T waitForObject(Producer<Optional<T>> predicate, String timeoutErrorMessage) {
        int msPassed = 0;
        while (msPassed <= TIMEOUT_MS) {
            final Optional<T> result = predicate.call();
            if (result != null && result.isPresent()) {
                return result.get();
            }
            msPassed += SLEEP_MS;
            sleep();
        }
        throw new AssertionError(timeoutErrorMessage);
    }

    private static void sleep() {
        try {
            Thread.sleep(WaitUtils.SLEEP_MS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
