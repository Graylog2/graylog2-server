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

/**
 * Based on <a href="https://github.com/LMAX-Exchange/disruptor/blob/3.4.4/src/main/java/com/lmax/disruptor/WorkHandler.java">the original WorkHandler interface</a>.
 * This was previously used for handlers which are part of a worker pool but the functionality was removed from the
 * disruptor library. We are keeping the interface but workers implementing it won't be able to use the batching
 * semantics that an {@link com.lmax.disruptor.EventHandler} provides.
 */
public interface WorkHandler<T> {
    void onEvent(T event) throws Exception;

    /**
     * Called once on thread start before first event is available.
     */
    default void onStart() {
    }

    /**
     * <p>Called once just before the thread is shutdown.</p>
     * <p>
     * Sequence event processing will already have stopped before this method is called. No events will
     * be processed after this message.
     */
    default void onShutdown() {
    }
}
