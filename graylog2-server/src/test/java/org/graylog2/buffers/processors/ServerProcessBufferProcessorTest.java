/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.buffers.processors;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Sets;
import org.graylog2.Configuration;
import org.graylog2.buffers.OutputBuffer;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.plugin.inputs.MessageInput;
import org.mockito.Matchers;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class ServerProcessBufferProcessorTest {

    @Test
    public void testFiltersAreOrdered() {
        final DummyFilter third = new DummyFilter(30);
        final DummyFilter first = new DummyFilter(10);
        final DummyFilter second = new DummyFilter(20);
        Set<MessageFilter> filters = Sets.<MessageFilter>newHashSet(
                third,
                first,
                second);
        final ServerProcessBufferProcessor processor = new ServerProcessBufferProcessor(mock(
                MetricRegistry.class), filters, mock(Configuration.class), 0, 1, mock(OutputBuffer.class));
        final List<MessageFilter> filterRegistry = processor.getFilterRegistry();

        assertEquals(filterRegistry.get(0), first);
        assertEquals(filterRegistry.get(1), second);
        assertEquals(filterRegistry.get(2), third);
    }

    @Test
    public void testHandleMessageEmptyFilterSet() throws Exception {
        MetricRegistry metricRegistry = mock(MetricRegistry.class);
        AtomicInteger processBufferWatermark = new AtomicInteger();
        OutputBuffer outputBuffer = mock(OutputBuffer.class);
        final Configuration configuration = mock(Configuration.class);
        when(configuration.isDisableOutputCache()).thenReturn(false);

        final ServerProcessBufferProcessor emptyFilters =
                new ServerProcessBufferProcessor(metricRegistry,
                                                 Sets.<MessageFilter>newHashSet(),
                                                 configuration,
                                                 0, 1,
                                                 outputBuffer);
        try {
            emptyFilters.handleMessage(new Message("test", "source", Tools.iso8601()));
            fail("A processor with empty filter set should throw an exception");
        } catch (RuntimeException ignored) {}
    }

    @Test
    public void testHandleMessage() {
        MetricRegistry metricRegistry = new MetricRegistry();
        AtomicInteger processBufferWatermark = new AtomicInteger();
        OutputBuffer outputBuffer = mock(OutputBuffer.class);
        final Configuration configuration = mock(Configuration.class);
        when(configuration.isDisableOutputCache()).thenReturn(false);

        MessageFilter filterOnlyFirst = new MessageFilter() {
            private boolean filterOut = true;

            @Override
            public boolean filter(Message msg) {
                if (filterOut) {
                    msg.setFilterOut(true);
                    filterOut = false;
                    return true;
                }
                return false;
            }

            @Override
            public String getName() {
                return "first filtered out, subsequent pass";
            }

            @Override
            public int getPriority() {
                return 0;
            }
        };

        final ServerProcessBufferProcessor filterTest =
                new ServerProcessBufferProcessor(metricRegistry,
                                                 Sets.newHashSet(filterOnlyFirst),
                                                 configuration,
                                                 0, 1,
                                                 outputBuffer);
        try {
            Message filteredoutMessage = new Message("filtered out", "source", Tools.iso8601());
            Message unfilteredMessage = new Message("filtered out", "source", Tools.iso8601());

            filterTest.handleMessage(filteredoutMessage);
            filterTest.handleMessage(unfilteredMessage);

            verify(outputBuffer, times(0)).insertCached(same(filteredoutMessage), Matchers.<MessageInput>anyObject());
            verify(outputBuffer, times(1)).insertCached(same(unfilteredMessage), Matchers.<MessageInput>anyObject());
            assertTrue(filteredoutMessage.getFilterOut());
            assertFalse(unfilteredMessage.getFilterOut());

        } catch (RuntimeException e) {
            fail("This test should not throw exceptions", e);
        }
    }

    private class DummyFilter implements MessageFilter {
        private final int prio;

        private DummyFilter(int prio) {
            this.prio = prio;
        }

        @Override
        public boolean filter(Message msg) {
            return false;
        }

        @Override
        public String getName() {
            return "filter prio " + prio;
        }

        @Override
        public int getPriority() {
            return prio;
        }
    }
}
