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

public class FailuresCounter {

    private int counter;
    private final int initialValue;
    private final int maxFailuresCount;

    private FailuresCounter(int initialValue, int maxFailuresCount) {
        this.maxFailuresCount = maxFailuresCount;
        this.initialValue = initialValue;
        resetFailuresCounter();
    }

    public static FailuresCounter oneBased(int maxFailuresCount) {
        return new FailuresCounter(1, maxFailuresCount);
    }

    public static FailuresCounter zeroBased(int maxFailuresCount) {
        return new FailuresCounter(0, maxFailuresCount);
    }

    public synchronized void increment() {
        this.counter++;
    }

    public synchronized boolean failedTooManyTimes() {
        return this.counter >= maxFailuresCount;
    }

    public synchronized void resetFailuresCounter() {
        this.counter = initialValue;
    }
}
