/**
 * Copyright 2011 Lennart Koopmann <lennart@socketfeed.com>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.graylog2;

import org.graylog2.plugin.Counter;

/**
 * A counter object with synchronized writes and utility methods.
 */
public class CounterImpl implements Counter, Comparable<Counter> {

    private int count = 0;

    public CounterImpl(int count) {
        this.set(count);
    }

    public CounterImpl(double count) {
        this.set(count);
    }

    public CounterImpl(Counter counter) {
        this.set(counter);
    }

    @Override
    public int get() {
        return this.count;
    }

    /**
     * Increment counter by 1.
     */
    @Override
    public void increment() {
        this.add(1);
    }

    /**
     * Reset counter to 0.
     */
    @Override
    public void reset() {
        this.set(0);
    }

    /**
     * Set counter to provided value.
     */
    @Override
    public void set(int value) {
        this.innerSet(value);
    }

    /**
     * Set counter to floored provided value.
     */
    @Override
    public void set(double value) {
        this.set(Double.valueOf(Math.floor(value)).intValue());
    }

    /**
     * Set counter to provided counter value. If null then do nothing.
     */
    @Override
    public void set(Counter value) {
        if (value != null) {
            this.set(value.get());
        }
    }

    /**
     * Add provided value to counter.
     */
    @Override
    public void add(int value) {
        this.innerAdd(value);
    }

    /**
     * Add floored provided value to counter.
     */
    @Override
    public void add(double value) {
        this.add(Double.valueOf(Math.floor(value)).intValue());
    }

    /**
     * Add provided counter value to counter. If null then do nothing.
     */
    @Override
    public void add(Counter value) {
        if (value != null) {
            this.add(value.get());
        }
    }

    @Override
    public String toString() {
        return "" + this.count;
    }

    @Override
    public boolean equals(Object comparedObject) {
        boolean isEqual = false;

        if (comparedObject != null) {
            if (comparedObject instanceof Counter) {
                isEqual = this.compareTo((Counter) comparedObject) == 0;
            }
        }

        return isEqual;
    }

    @Override
    public int compareTo(Counter comparedCounter) {
        int compareResult = this.get();

        if (comparedCounter != null) {
            compareResult = this.get() - comparedCounter.get();
        }

        return compareResult;
    }

    private synchronized void innerAdd(int value) {
        this.count += value;
    }

    private synchronized void innerSet(int value) {
        this.count = value;
    }
}
