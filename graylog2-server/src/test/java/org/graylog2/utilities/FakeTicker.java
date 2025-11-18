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
package org.graylog2.utilities;

import com.github.benmanes.caffeine.cache.Ticker;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class FakeTicker implements Ticker {

    private final AtomicLong nanos = new AtomicLong();
    private final long autoIncrementStepNanos;

    /**
     * Create a new controlled ticker for use with Caffeine caches
     *
     * @param autoIncrementStep each read advances time by this amount (can be 0)
     */
    public FakeTicker(Duration autoIncrementStep) {
        Objects.requireNonNull(autoIncrementStep, "autoIncrementStep cannot be null");
        if (autoIncrementStep.isNegative()) {
            throw new IllegalArgumentException("autoIncrementStep cannot be negative");
        }
        this.autoIncrementStepNanos = autoIncrementStep.toNanos();
    }

    /**
     * Advances the ticker value by {@code duration}.
     * The duration can be negative!
     */
    public FakeTicker advance(Duration duration) {
        return advance(duration.toNanos());
    }

    /**
     * Advances the ticker value by {@code nanoseconds}.
     */
    public FakeTicker advance(long nanoseconds) {
        nanos.addAndGet(nanoseconds);
        return this;
    }

    @Override
    public long read() {
        return nanos.getAndAdd(autoIncrementStepNanos);
    }
}
