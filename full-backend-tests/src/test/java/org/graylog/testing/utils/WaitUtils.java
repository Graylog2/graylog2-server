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

import static org.junit.Assert.fail;

public final class WaitUtils {

    private WaitUtils() {}

    public static void waitFor(Producer<Boolean> predicate, String timeoutErrorMessage) {
        int timeOutMs = 10000;
        int msPassed = 0;
        int waitMs = 500;
        while (msPassed <= timeOutMs) {
            if (predicate.call()) {
                return;
            }
            msPassed += waitMs;
            wait(waitMs);
        }
        fail(timeoutErrorMessage);
    }

    private static void wait(int waitMs) {
        try {
            Thread.sleep(waitMs);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
