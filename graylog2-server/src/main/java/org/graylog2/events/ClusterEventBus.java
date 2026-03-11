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
package org.graylog2.events;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.SubscriberExceptionHandler;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.Executor;

public class ClusterEventBus extends AsyncEventBus {
    public ClusterEventBus () {
        this(MoreExecutors.directExecutor());
    }

    public ClusterEventBus(Executor executor) {
        super(executor);
    }

    public ClusterEventBus(Executor executor, SubscriberExceptionHandler subscriberExceptionHandler) {
        super(executor, subscriberExceptionHandler);
    }

    public ClusterEventBus(String identifier, Executor executor) {
        super(identifier, executor);
    }

    @Override
    public void register(Object object) {
        throw new IllegalStateException("Do not use ClusterEventBus for regular subscriptions. You probably want to use the regular EventBus.");
    }

    /**
     * Registers a subscriber that only receives events posted on the local node.
     *
     * <p>Events posted to {@link ClusterEventBus} are dispatched to subscribers registered here
     * <strong>before</strong> they are persisted and replicated to other nodes. On remote nodes,
     * {@link org.graylog2.events.ClusterEventPeriodical} polls MongoDB and posts the replicated
     * events to the server {@link com.google.common.eventbus.EventBus} instead, so subscribers
     * registered here will <strong>not</strong> see those replicated events.</p>
     *
     * <p>Use this when a handler must run exactly once on the originating node (e.g. reacting to
     * a license install). For cluster-wide delivery, subscribe on the server EventBus.</p>
     *
     * @param object the subscriber to register
     */
    public void registerClusterEventSubscriber(Object object) {
        super.register(object);
    }
}
