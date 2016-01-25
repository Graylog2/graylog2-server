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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import org.graylog2.plugin.Messages;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.messageprocessors.MessageProcessor;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

public class OrderedMessageProcessorsTest {

    private OrderedMessageProcessors orderedMessageProcessors;

    @Before
    public void setUp() throws Exception {
        Set<MessageProcessor> processors = Sets.newHashSet();
        processors.add(new A());
        processors.add(new B());
        orderedMessageProcessors = new OrderedMessageProcessors(processors,
                                                                mock(ClusterConfigService.class),
                                                                mock(EventBus.class));
    }

    @Test
    public void testIterator() throws Exception {
        final Iterator<MessageProcessor> iterator = orderedMessageProcessors.iterator();
        assertEquals("A is first", A.class, iterator.next().getClass());
        assertEquals("B is last", B.class, iterator.next().getClass());
        assertFalse("Iterator exhausted", iterator.hasNext());

        orderedMessageProcessors.handleOrderingUpdate(
                MessageProcessorOrder.create(1, Lists.newArrayList(B.class.getCanonicalName(),
                                                                   A.class.getCanonicalName())));

        final Iterator<MessageProcessor> it2 = orderedMessageProcessors.iterator();
        assertEquals("B is first", B.class, it2.next().getClass());
        assertEquals("A is last", A.class, it2.next().getClass());
        assertFalse("Iterator exhausted", it2.hasNext());


        orderedMessageProcessors.handleOrderingUpdate(
                MessageProcessorOrder.create(1, Lists.newArrayList(B.class.getCanonicalName(),
                                                                   A.class.getCanonicalName()),
                                             Sets.newHashSet(B.class.getCanonicalName())));

        final Iterator<MessageProcessor> it3 = orderedMessageProcessors.iterator();
        assertEquals("A is only element", A.class, it3.next().getClass());
        assertFalse("Iterator exhausted", it3.hasNext());
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