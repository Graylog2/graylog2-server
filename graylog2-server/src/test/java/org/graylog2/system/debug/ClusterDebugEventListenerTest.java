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

import org.graylog2.events.ClusterEventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;

public class ClusterDebugEventListenerTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Spy
    private ClusterEventBus clusterEventBus;

    @Before
    public void setUp() {
        DebugEventHolder.setClusterDebugEvent(null);
        new ClusterDebugEventListener(clusterEventBus);
    }

    @Test
    public void testHandleDebugEvent() throws Exception {
        DebugEvent event = DebugEvent.create("Node ID", "Test");
        assertThat(DebugEventHolder.getClusterDebugEvent()).isNull();
        clusterEventBus.post(event);
        assertThat(DebugEventHolder.getClusterDebugEvent()).isSameAs(event);
    }
}