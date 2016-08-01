/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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