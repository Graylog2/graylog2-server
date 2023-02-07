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
     * Only use this if you maintain the cluster event bus! Use regular EventBus to receive cluster event updates.
     * @param object
     */
    public void registerClusterEventSubscriber(Object object) {
        super.register(object);
    }
}
