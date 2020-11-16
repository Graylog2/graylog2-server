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
package org.graylog2.shared.events;

import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeadEventLoggingListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeadEventLoggingListener.class);

    @Subscribe
    public void handleDeadEvent(DeadEvent event) {
        LOGGER.debug("Received unhandled event of type <{}> from event bus <{}>", event.getEvent().getClass().getCanonicalName(),
                event.getSource().toString());
        LOGGER.debug("Dead event contents: {}", event.getEvent());
    }
}
