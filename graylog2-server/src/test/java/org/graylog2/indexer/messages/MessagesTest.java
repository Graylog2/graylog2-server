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
package org.graylog2.indexer.messages;

import com.google.common.collect.ImmutableList;
import org.graylog2.indexer.IndexSet;
import org.graylog2.plugin.Message;
import org.graylog2.system.processing.ProcessingStatusRecorder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MessagesTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private MessagesAdapter messagesAdapter;

    @Mock
    private TrafficAccounting trafficAccounting;

    private Messages messages;

    @Before
    public void setUp() throws Exception {
        this.messages = new Messages(trafficAccounting, messagesAdapter, mock(ProcessingStatusRecorder.class));
    }

    @Test
    public void bulkIndexingShouldNotDoAnythingForEmptyList() throws Exception {
        final List<String> result = messages.bulkIndex(Collections.emptyList());

        assertThat(result).isNotNull()
                .isEmpty();

        verify(messagesAdapter, never()).bulkIndex(any());
    }

    @Test
    public void bulkIndexingShouldAccountMessageSizes() {
        when(messagesAdapter.bulkIndex(any())).thenReturn(Collections.emptyList());
        final IndexSet indexSet = mock(IndexSet.class);
        final List<Map.Entry<IndexSet, Message>> messageList = ImmutableList.of(
                createMessageListEntry(indexSet, messageWithSize(17)),
                createMessageListEntry(indexSet, messageWithSize(23)),
                createMessageListEntry(indexSet, messageWithSize(42))
        );

        messages.bulkIndex(messageList);

        verify(trafficAccounting, times(1)).addOutputTraffic(82);
        verify(trafficAccounting, never()).addSystemTraffic(anyLong());
    }

    @Test
    public void bulkIndexingShouldAccountMessageSizesForSystemTrafficSeparately() {
        when(messagesAdapter.bulkIndex(any())).thenReturn(Collections.emptyList());
        final IndexSet indexSet = mock(IndexSet.class);
        final List<Map.Entry<IndexSet, Message>> messageList = ImmutableList.of(
                createMessageListEntry(indexSet, messageWithSize(17)),
                createMessageListEntry(indexSet, messageWithSize(23)),
                createMessageListEntry(indexSet, messageWithSize(42))
        );

        messages.bulkIndex(messageList, true);

        verify(trafficAccounting, never()).addOutputTraffic(anyLong());
        verify(trafficAccounting, times(1)).addSystemTraffic(82);
    }

    private Map.Entry<IndexSet, Message> createMessageListEntry(IndexSet indexSet, Message message) {
        return new AbstractMap.SimpleEntry<>(indexSet, message);
    }

    private Message messageWithSize(long size) {
        final Message message = mock(Message.class);
        when(message.getSize()).thenReturn(size);

        return message;
    }
}
