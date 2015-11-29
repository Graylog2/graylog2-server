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
package org.graylog2.messageprocessors;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Messages;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.shared.journal.Journal;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class MessageFilterChainProcessorTest {
    @Mock
    private ServerStatus serverStatus;

    @Before
    public void setUp() throws Exception {
        Mockito.when(serverStatus.getDetailedMessageRecordingStrategy()).thenReturn(ServerStatus.MessageDetailRecordingStrategy.NEVER);
    }

    @Test
    public void testFiltersAreOrdered() {
        final DummyFilter third = new DummyFilter(30);
        final DummyFilter first = new DummyFilter(10);
        final DummyFilter second = new DummyFilter(20);
        Set<MessageFilter> filters = Sets.<MessageFilter>newHashSet(
                third,
                first,
                second);
        final MessageFilterChainProcessor processor = new MessageFilterChainProcessor(Mockito.mock(MetricRegistry.class),
                                                                                      filters,
                                                                                      Mockito.mock(Journal.class),
                                                                                      serverStatus);
        final List<MessageFilter> filterRegistry = processor.getFilterRegistry();

        Assert.assertEquals(filterRegistry.get(0), first);
        Assert.assertEquals(filterRegistry.get(1), second);
        Assert.assertEquals(filterRegistry.get(2), third);
    }

    @Test
    public void testHandleMessageEmptyFilterSet() throws Exception {
        try {
            new MessageFilterChainProcessor(new MetricRegistry(),
                                            Sets.newHashSet(),
                                            Mockito.mock(Journal.class),
                                            serverStatus);
            Assert.fail("A processor without message filters should fail on creation");
        } catch (RuntimeException ignored) {}
    }

    @Test
    public void testHandleMessage() {

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

        final MessageFilterChainProcessor filterTest = new MessageFilterChainProcessor(new MetricRegistry(),
                                                                                       Sets.newHashSet(filterOnlyFirst),
                                                                                       Mockito.mock(Journal.class),
                                                                                       serverStatus);
        Message filteredoutMessage = new Message("filtered out", "source", Tools.iso8601());
        filteredoutMessage.setJournalOffset(1);
        Message unfilteredMessage = new Message("filtered out", "source", Tools.iso8601());

        final Messages messages1 = filterTest.process(filteredoutMessage);
        final Messages messages2 = filterTest.process(unfilteredMessage);

        Assert.assertTrue(filteredoutMessage.getFilterOut());
        Assert.assertFalse(unfilteredMessage.getFilterOut());
        Assert.assertEquals(0, Iterables.size(messages1));
        Assert.assertEquals(1, Iterables.size(messages2));
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
