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
package org.graylog2.plugin.inputs.transports;

import com.google.common.eventbus.EventBus;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;

/**
 * Newer version of the {@link ThrottleableTransport} which launches
 * with an {@link InputFailureRecorder}
 */
public abstract class ThrottleableTransport2 extends ThrottleableTransport {

    public ThrottleableTransport2(EventBus eventBus, Configuration configuration) {
        super(eventBus, configuration);
    }

    @Override
    public void launch(MessageInput input, InputFailureRecorder inputFailureRecorder) throws MisfireException {
        // Call this before registering on the event bus. There might be stuff in doLaunch() that needs to run first.
        doLaunch(input, inputFailureRecorder);

        // only listen for updates if we are allowed to be throttled at all
        if (throttlingAllowed) {
            eventBus.register(this);
        }
    }

    @Override
    protected final void doLaunch(MessageInput input) throws MisfireException {
        throw new MisfireException("Launch without InputFailureTracker");
    }

    protected abstract void doLaunch(MessageInput input, InputFailureRecorder inputFailureRecorder) throws MisfireException;

}
