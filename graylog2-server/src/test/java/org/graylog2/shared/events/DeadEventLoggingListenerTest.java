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
import com.google.common.eventbus.EventBus;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DeadEventLoggingListenerTest {

    @Test
    public void testHandleDeadEvent() {
        final DeadEventLoggingListener listener = new DeadEventLoggingListener();
        final DeadEvent event = new DeadEvent(this, new SimpleEvent("test"));

        listener.handleDeadEvent(event);
    }

    @Test
    public void testEventListenerWithEventBus() {
        final EventBus eventBus = new EventBus("test");
        final SimpleEvent event = new SimpleEvent("test");
        final DeadEventLoggingListener listener = spy(new DeadEventLoggingListener());
        eventBus.register(listener);

        eventBus.post(event);

        verify(listener, times(1)).handleDeadEvent(any(DeadEvent.class));
    }

    public static class SimpleEvent {
        public String payload;

        public SimpleEvent(String payload) {
            this.payload = payload;
        }

        @Override
        public String toString() {
            return "payload=" + payload;
        }
    }
}