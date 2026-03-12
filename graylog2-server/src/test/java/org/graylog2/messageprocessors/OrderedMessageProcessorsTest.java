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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import org.graylog2.cluster.ClusterConfigChangedEvent;
import org.graylog2.plugin.Messages;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.messageprocessors.MessageProcessor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OrderedMessageProcessorsTest {

    private OrderedMessageProcessors orderedMessageProcessors;
    private ClusterConfigService clusterConfigService;

    @BeforeEach
    public void setUp() throws Exception {
        Set<MessageProcessor> processors = Sets.newHashSet();
        processors.add(new A());
        processors.add(new B());
        clusterConfigService = mock(ClusterConfigService.class);
        orderedMessageProcessors = new OrderedMessageProcessors(processors,
                                                                clusterConfigService,
                                                                mock(EventBus.class));
    }

    private ClusterConfigChangedEvent getClusterConfigChangedEvent() {
        return ClusterConfigChangedEvent.create(DateTime.now(DateTimeZone.UTC), "node-id", MessageProcessorsConfig.class.getCanonicalName());
    }

    @Test
    public void testIterator() throws Exception {
        final Iterator<MessageProcessor> iterator = orderedMessageProcessors.iterator();
        assertEquals(A.class, iterator.next().getClass(), "A is first");
        assertEquals(B.class, iterator.next().getClass(), "B is last");
        assertFalse(iterator.hasNext(), "Iterator exhausted");

        when(clusterConfigService.get(MessageProcessorsConfig.class)).thenReturn(
                MessageProcessorsConfig.create(Lists.newArrayList(B.class.getCanonicalName(),
                                                                  A.class.getCanonicalName())));

        orderedMessageProcessors.handleOrderingUpdate(getClusterConfigChangedEvent());

        final Iterator<MessageProcessor> it2 = orderedMessageProcessors.iterator();
        assertEquals(B.class, it2.next().getClass(), "B is first");
        assertEquals(A.class, it2.next().getClass(), "A is last");
        assertFalse(it2.hasNext(), "Iterator exhausted");


        when(clusterConfigService.get(MessageProcessorsConfig.class)).thenReturn(
                MessageProcessorsConfig.create(Lists.newArrayList(B.class.getCanonicalName(),
                                                                  A.class.getCanonicalName()),
                                               Sets.newHashSet(B.class.getCanonicalName())));

        orderedMessageProcessors.handleOrderingUpdate(getClusterConfigChangedEvent());

        final Iterator<MessageProcessor> it3 = orderedMessageProcessors.iterator();
        assertEquals(A.class, it3.next().getClass(), "A is only element");
        assertFalse(it3.hasNext(), "Iterator exhausted");
    }

    private static class A implements MessageProcessor {
        @Override
        public Messages process(Messages messages) {
            return null;
        }
    }

    private static class B implements MessageProcessor {
        @Override
        public Messages process(Messages messages) {
            return null;
        }
    }
}