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
package org.graylog.plugins.views.search.engine.monitoring.collection;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.ArrayList;
import java.util.Collection;

public class InMemoryCappedQueryExecutionStatsCollector<T> implements QueryExecutionStatsCollector<T> {

    private final CircularFifoQueue<T> cappedQueue;

    public InMemoryCappedQueryExecutionStatsCollector(final int maxSize) {
        this.cappedQueue = new CircularFifoQueue<>(maxSize); //TODO: max size should be configurable
    }

    @Override
    public void storeStats(final T stats) {
        synchronized (cappedQueue) {
            cappedQueue.add(stats);
        }
    }

    @Override
    public Collection<T> getAllStats() {
        synchronized (cappedQueue) {
            return new ArrayList<>(cappedQueue);
        }
    }
}
