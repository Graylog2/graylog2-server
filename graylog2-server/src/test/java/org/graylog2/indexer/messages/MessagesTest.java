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
import org.graylog.failure.FailureSubmissionService;
import org.graylog2.indexer.IndexSet;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.system.processing.ProcessingStatusRecorder;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class MessagesTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private MessagesAdapter messagesAdapter;

    @Mock
    private TrafficAccounting trafficAccounting;

    @Mock
    private FailureSubmissionService failureSubmissionService;

    @Captor
    private ArgumentCaptor<Collection<Messages.IndexingError>> indexingErrorsArgumentCaptor;

    private Messages messages;

    @Before
    public void setUp() throws Exception {
        this.messages = new Messages(trafficAccounting, messagesAdapter, mock(ProcessingStatusRecorder.class), failureSubmissionService);
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

    @Test
    public void bulkIndexRequests_allNonIndexBlockErrorsPropagatedToTheFailureSubmissionService() throws Exception {
        // given
        final DateTime ts = Tools.nowUTC();
        final IndexSet indexSet = mock(IndexSet.class);
        final Message message1 = mock(Message.class);
        final Message message2 = message("msg-2", ts);
        final Message message3 = message("msg-3", ts);
        final Message message4 = message("msg-4", ts);

        final List<IndexingRequest> indexingRequest = ImmutableList.of(
                IndexingRequest.create(indexSet, message1),
                IndexingRequest.create(indexSet, message2),
                IndexingRequest.create(indexSet, message3));

        when(messagesAdapter.bulkIndex(indexingRequest)).thenReturn(
                ImmutableList.of(
                        Messages.IndexingError.create(message2, "msg-index", Messages.IndexingError.ErrorType.MappingError, "Some error message"),
                        Messages.IndexingError.create(message3, "msg-index", Messages.IndexingError.ErrorType.MappingError, "Some error message"),
                        Messages.IndexingError.create(message4, "msg-index", Messages.IndexingError.ErrorType.IndexBlocked, "Index blocked error message")
                )
        );

        // when
        final List<String> failureIds = messages.bulkIndexRequests(indexingRequest, false);

        // then
        assertThat(failureIds).hasSize(2)
                .containsExactlyInAnyOrder("msg-2", "msg-3");

        verify(failureSubmissionService, times(1)).submitIndexingErrors(indexingErrorsArgumentCaptor.capture());

        assertThat(indexingErrorsArgumentCaptor.getValue()
                .stream()
                .sorted(Comparator.comparing(e -> e.message().getMessageId()))
                .collect(Collectors.toList())
        ).satisfies(indexingErrors -> {
            assertThat(indexingErrors.get(0)).satisfies(indexingError -> {
                assertThat(indexingError.errorType()).isEqualTo(Messages.IndexingError.ErrorType.MappingError);
                assertThat(indexingError.message()).isEqualTo(message2);
            });

            assertThat(indexingErrors.get(1)).satisfies(indexingError -> {
                assertThat(indexingError.errorType()).isEqualTo(Messages.IndexingError.ErrorType.MappingError);
                assertThat(indexingError.message()).isEqualTo(message3);
            });
        });
    }

    @Test
    public void bulkIndexRequests_nothingPropagatedToFailureSubmissionServiceWhenThereAreNoIndexingErrors() throws Exception {
        // given
        final DateTime ts = Tools.nowUTC();
        final IndexSet indexSet = mock(IndexSet.class);
        final Message message1 = message("msg-1", ts);
        final Message message2 = message("msg-2", ts);

        final List<IndexingRequest> indexingRequest = ImmutableList.of(
                IndexingRequest.create(indexSet, message1),
                IndexingRequest.create(indexSet, message2));

        when(messagesAdapter.bulkIndex(indexingRequest)).thenReturn(ImmutableList.of());

        // when
        final List<String> failureIds = messages.bulkIndexRequests(indexingRequest, false);

        // then
        assertThat(failureIds).isEmpty();

        verifyNoInteractions(failureSubmissionService);
    }

    private Message message(String msgId, DateTime ts) {
        final Message mock = mock(Message.class);
        when(mock.getId()).thenReturn(msgId);
        when(mock.getMessageId()).thenReturn(msgId);
        when(mock.getTimestamp()).thenReturn(ts);
        return mock;
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
