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
package org.graylog2.messageprocessors;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Messages;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.shared.messageq.MessageQueueAcknowledger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageFilterChainProcessorTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ServerStatus serverStatus;
    @Mock
    private MessageQueueAcknowledger acknowledger;

    @Before
    public void setUp() throws Exception {
        Mockito.when(serverStatus.getDetailedMessageRecordingStrategy()).thenReturn(ServerStatus.MessageDetailRecordingStrategy.NEVER);
    }

    @Test
    public void testFiltersAreOrdered() {
        final DummyFilter third = new DummyFilter(30);
        final DummyFilter first = new DummyFilter(10);
        final DummyFilter second = new DummyFilter(20);
        final Set<MessageFilter> filters = ImmutableSet.of(third, first, second);
        final MessageFilterChainProcessor processor = new MessageFilterChainProcessor(new MetricRegistry(),
                                                                                      filters,
                                                                                      acknowledger,
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
                                            Collections.emptySet(),
                                            acknowledger,
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
                                                                                       Collections.singleton(filterOnlyFirst),
                                                                                       acknowledger,
                                                                                       serverStatus);
        Message filteredoutMessage = new Message("filtered out", "source", Tools.nowUTC());
        filteredoutMessage.setJournalOffset(1);
        Message unfilteredMessage = new Message("filtered out", "source", Tools.nowUTC());

        final Messages messages1 = filterTest.process(filteredoutMessage);
        final Messages messages2 = filterTest.process(unfilteredMessage);

        Assert.assertTrue(filteredoutMessage.getFilterOut());
        Assert.assertFalse(unfilteredMessage.getFilterOut());
        Assert.assertEquals(0, Iterables.size(messages1));
        Assert.assertEquals(1, Iterables.size(messages2));
    }

    @Test
    public void testAllFiltersAreBeingRun() {
        final DummyFilter first = new DummyFilter(10);
        final DummyFilter second = new DummyFilter(20);
        final DummyFilter third = new DummyFilter(30);
        final Set<MessageFilter> filters = ImmutableSet.of(first, second, third);
        final MessageFilterChainProcessor processor = new MessageFilterChainProcessor(new MetricRegistry(),
                filters,
                acknowledger,
                serverStatus);

        final Message message = new Message("message", "source", new DateTime(2016, 1, 1, 0, 0, DateTimeZone.UTC));
        final Message result = Iterables.getFirst(processor.process(message), null);

        assertThat(result).isNotNull();
        assertThat(result.getFields()).containsKeys("prio-10", "prio-20", "prio-30");
    }

    @Test
    public void testMessagesCanBeDropped() {
        final MessageFilter first = new DummyFilter(10);
        final MessageFilter second = new RemovingMessageFilter();
        final Set<MessageFilter> filters = ImmutableSet.of(first, second);
        final MessageFilterChainProcessor processor = new MessageFilterChainProcessor(new MetricRegistry(),
                filters,
                acknowledger,
                serverStatus);

        final Message message = new Message("message", "source", new DateTime(2016, 1, 1, 0, 0, DateTimeZone.UTC));
        final Messages result = processor.process(message);

        assertThat(result).isEmpty();
    }

    private static class DummyFilter implements MessageFilter {
        private final int prio;

        private DummyFilter(int prio) {
            this.prio = prio;
        }

        @Override
        public boolean filter(Message msg) {
            msg.addField("prio-" + prio, true);
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

    private static class RemovingMessageFilter implements MessageFilter {
        @Override
        public boolean filter(Message msg) {
            return true;
        }

        @Override
        public String getName() {
            return "Removing filter";
        }

        @Override
        public int getPriority() {
            return Integer.MAX_VALUE;
        }
    }
}
