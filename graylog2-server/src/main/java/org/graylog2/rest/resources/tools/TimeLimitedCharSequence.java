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
package org.graylog2.rest.resources.tools;

import java.time.Clock;

public final class TimeLimitedCharSequence implements CharSequence {
    public static final long DEFAULT_TIMEOUT_MS = 500;

    private final CharSequence inner;
    private final long deadline;
    private final Clock clock;

    private TimeLimitedCharSequence(final CharSequence inner, final long deadline, final Clock clock) {
        this.inner = inner;
        this.deadline = deadline;
        this.clock = clock;
    }

    public static TimeLimitedCharSequence withTimeout(final CharSequence inner, final long timeoutMs, final Clock clock) {
        return new TimeLimitedCharSequence(inner, clock.millis() + timeoutMs, clock);
    }

    @Override
    public int length() {
        return inner.length();
    }

    @Override
    public char charAt(final int index) {
        if (clock.millis() > deadline) {
            throw new TimeoutException();
        }
        return inner.charAt(index);
    }

    @Override
    public CharSequence subSequence(final int start, final int end) {
        return new TimeLimitedCharSequence(inner.subSequence(start, end), deadline, clock);
    }

    @Override
    public String toString() {
        return inner.toString();
    }

    public static final class TimeoutException extends RuntimeException {
        TimeoutException() {
            super("Regex matching time limit exceeded", null, true, false);
        }
    }
}
