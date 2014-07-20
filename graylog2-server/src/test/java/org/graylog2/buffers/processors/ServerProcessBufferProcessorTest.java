package org.graylog2.buffers.processors;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Sets;
import org.graylog2.buffers.OutputBuffer;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.plugin.inputs.MessageInput;
import org.joda.time.DateTime;
import org.mockito.Matchers;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

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
                MetricRegistry.class), filters, new AtomicInteger(), 0, 1, mock(OutputBuffer.class));
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

        final ServerProcessBufferProcessor emptyFilters =
                new ServerProcessBufferProcessor(metricRegistry,
                                                 Sets.<MessageFilter>newHashSet(),
                                                 processBufferWatermark, 0, 1,
                                                 outputBuffer);
        try {
            emptyFilters.handleMessage(new Message("test", "source", DateTime.now()));
            fail("A processor with empty filter set should throw an exception");
        } catch (RuntimeException ignored) {}
    }

    @Test
    public void testHandleMessage() {
        MetricRegistry metricRegistry = new MetricRegistry();
        AtomicInteger processBufferWatermark = new AtomicInteger();
        OutputBuffer outputBuffer = mock(OutputBuffer.class);

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
                                                 processBufferWatermark, 0, 1,
                                                 outputBuffer);
        try {
            Message filteredoutMessage = new Message("filtered out", "source", DateTime.now());
            Message unfilteredMessage = new Message("filtered out", "source", DateTime.now());

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