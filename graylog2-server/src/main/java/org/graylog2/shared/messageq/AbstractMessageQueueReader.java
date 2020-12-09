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
package org.graylog2.shared.messageq;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import org.graylog2.plugin.lifecycles.Lifecycle;

public abstract class AbstractMessageQueueReader extends AbstractExecutionThreadService  implements MessageQueueReader {
    private final EventBus eventBus;
    private volatile boolean shouldBeReading;

    public AbstractMessageQueueReader(EventBus eventBus) {
        this.eventBus = eventBus;
        shouldBeReading = false;
    }

    @Override
    protected void startUp() throws Exception {
        super.startUp();
        eventBus.register(this);
    }

    @Override
    protected void shutDown() throws Exception {
        eventBus.unregister(this);
        super.shutDown();
    }

    @Subscribe
    public void listenForLifecycleChanges(Lifecycle lifecycle) {
        switch (lifecycle) {
            case UNINITIALIZED:
            case STARTING:
            case PAUSED:
            case HALTING:
                shouldBeReading = false;
                break;
            case RUNNING:
            case THROTTLED:
                shouldBeReading = true;
                break;
            case FAILED:
                triggerShutdown();
                break;
            default:
                // don't care, keep processing journal
                break;
        }
    }

    /**
     * Indicates if the reader should read from the message queue or if it should currently pause reading. The
     * returned value is affected by lifecycle changes, e.g. during server startup or when processing has stopped it
     * will be false, during normal operation mode it will be true.
     */
    protected boolean shouldBeReading() {
        return shouldBeReading;
    }
}
