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
package org.graylog2.system.debug;

import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;

public class LocalDebugEventListenerTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Spy
    private EventBus serverEventBus;

    @Before
    public void setUp() {
        DebugEventHolder.setLocalDebugEvent(null);
        new LocalDebugEventListener(serverEventBus);
    }

    @Test
    public void testHandleDebugEvent() throws Exception {
        DebugEvent event = DebugEvent.create("Node ID", "Test");
        assertThat(DebugEventHolder.getLocalDebugEvent()).isNull();
        serverEventBus.post(event);
        assertThat(DebugEventHolder.getLocalDebugEvent()).isSameAs(event);
    }
}