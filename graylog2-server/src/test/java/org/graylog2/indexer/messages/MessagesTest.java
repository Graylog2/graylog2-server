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

import java.io.IOException;
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
    public void bulkIndexingShouldAccountMessageSizes() throws IOException {
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
    public void bulkIndexingShouldAccountMessageSizesForSystemTrafficSeparately() throws IOException {
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
