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
package org.graylog.datanode.process;

/**
 * Caution, starts initialized with 1, the usage expects that the checks and increments will happen already during
 * the first error handling.
 */
public class FailuresCounter {

    private int counter;
    private final int maxFailuresCount;

    public FailuresCounter(int maxFailuresCount) {
        this.maxFailuresCount = maxFailuresCount;
        resetFailuresCounter();
    }

    public synchronized void increment() {
        this.counter++;
    }

    public synchronized boolean failedTooManyTimes() {
        return this.counter >= maxFailuresCount;
    }

    public synchronized void resetFailuresCounter() {
        this.counter = 1;
    }
}
