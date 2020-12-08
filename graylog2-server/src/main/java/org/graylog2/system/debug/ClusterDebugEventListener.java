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
package org.graylog2.system.debug;

import com.google.common.eventbus.Subscribe;
import org.graylog2.events.ClusterEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;

public class ClusterDebugEventListener {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterDebugEventListener.class);

    @Inject
    public ClusterDebugEventListener(ClusterEventBus clusterEventBus) {
        checkNotNull(clusterEventBus).registerClusterEventSubscriber(this);
    }

    @Subscribe
    public void handleDebugEvent(DebugEvent event) {
        LOG.debug("Received cluster debug event: {}", event);
        DebugEventHolder.setClusterDebugEvent(event);
    }
}
