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
package org.graylog2.shared.buffers;

import com.lmax.disruptor.EventHandler;

/**
 * An event handler which will only process a partition of events and which will delegate to {@link WorkHandler}. It
 * won't make use of batching semantics, therefore it's preferable to implement an {@link EventHandler} and handle
 * partitioning yourself.
 * <p>
 * Partitioning semantics are implemented as suggested
 * <a href="https://github.com/LMAX-Exchange/disruptor/wiki/Frequently-Asked-Questions#how-do-you-arrange-a-disruptor-with-multiple-consumers-so-that-each-event-is-only-consumed-once">in the disruptor FAQ</a>.
 */
public class PartitioningWorkHandler<S extends WorkHandler<T>, T> implements EventHandler<T> {
    private final S delegate;
    protected final long ordinal;
    protected long numberOfConsumers;

    /**
     * Create an event handler which will only handle a partition of events and doesn't provide support for batching.
     *
     * @param delegate          A WorkHandler which is
     * @param ordinal           The ordinal number of this consumer in the range [0, numberOfConsumers). Each event
     *                          handlers need to have a distinct ordinal number.
     * @param numberOfConsumers The total number of consumers.
     */
    public PartitioningWorkHandler(S delegate, long ordinal, long numberOfConsumers) {
        this.delegate = delegate;
        this.ordinal = ordinal;
        this.numberOfConsumers = numberOfConsumers;
    }


    @Override
    public final void onEvent(T event, long sequence, boolean endOfBatch) throws Exception {
        if ((sequence % numberOfConsumers) == ordinal) {
            delegate.onEvent(event);
        }
    }

    @Override
    public void onStart() {
        delegate.onStart();
    }

    @Override
    public void onShutdown() {
        delegate.onShutdown();
    }

    public S getDelegate() {
        return delegate;
    }
}
