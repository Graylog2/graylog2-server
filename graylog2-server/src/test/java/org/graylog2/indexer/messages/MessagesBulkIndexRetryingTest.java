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
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessagesBulkIndexRetryingTest {
    private final TrafficAccounting trafficAccounting = mock(TrafficAccounting.class);
    private final MessagesAdapter messagesAdapter = mock(MessagesAdapter.class);
    private final ProcessingStatusRecorder processingStatusRecorder = mock(ProcessingStatusRecorder.class);

    private Messages messages;

    @BeforeEach
    void setUp() {
        this.messages = new Messages(trafficAccounting, messagesAdapter, processingStatusRecorder);
    }

    @Test
    public void bulkIndexingShouldNotDoAnythingForEmptyList() throws Exception {
        final List<String> result = messages.bulkIndex(Collections.emptyList());

        assertThat(result).isNotNull()
                .isEmpty();

        verify(messagesAdapter, never()).bulkIndex(any());
    }

    @Test
    public void bulkIndexingShouldNotRetryForIndexMappingErrors() throws Exception {
        final String messageId = "BOOMID";

        final List<Messages.IndexingError> errorResult = ImmutableList.of(
                errorResultItem(messageId, Messages.IndexingError.ErrorType.MappingError, "failed to parse [http_response_code]")
        );

        when(messagesAdapter.bulkIndex(any()))
                .thenReturn(errorResult)
                .thenThrow(new IllegalStateException("JestResult#execute should not be called twice."));

        final Message mockedMessage = mock(Message.class);
        when(mockedMessage.getId()).thenReturn(messageId);
        when(mockedMessage.getTimestamp()).thenReturn(DateTime.now(DateTimeZone.UTC));

        final List<Map.Entry<IndexSet, Message>> messageList = messageListWith(mockedMessage);

        final List<String> result = messages.bulkIndex(messageList);

        assertThat(result).hasSize(1);

        verify(messagesAdapter, times(1)).bulkIndex(any());
    }

    @Test
    public void bulkIndexingShouldRetry() throws Exception {
        when(messagesAdapter.bulkIndex(any()))
                .thenThrow(new IOException("Boom!"))
                .thenReturn(Collections.emptyList());

        final List<Map.Entry<IndexSet, Message>> messageList = messageListWith(mock(Message.class));

        final List<String> result = messages.bulkIndex(messageList);

        assertThat(result).isNotNull().isEmpty();

        verify(messagesAdapter, times(2)).bulkIndex(any());
    }

    @Test
    public void bulkIndexingShouldRetryIfIndexBlocked() throws IOException {
        final List<Messages.IndexingError> errorResult = Collections.singletonList(
                errorResultItem("blocked-id", Messages.IndexingError.ErrorType.IndexBlocked, "Index is read-only")
        );
        final List<Messages.IndexingError> successResult = Collections.emptyList();

        when(messagesAdapter.bulkIndex(any()))
                .thenReturn(errorResult)
                .thenReturn(successResult);

        final List<String> result = messages.bulkIndex(messagesWithIds("blocked-id"));

        verify(messagesAdapter, times(2)).bulkIndex(any());
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    public void indexBlockedRetriesShouldOnlyRetryIndexBlockedErrors() throws IOException {
        final List<Messages.IndexingError> errorResult = ImmutableList.of(
                errorResultItem("blocked-id", Messages.IndexingError.ErrorType.IndexBlocked, "Index is read-only"),
                errorResultItem("other-error-id", Messages.IndexingError.ErrorType.Unknown, "Some other error")
        );
        final List<Messages.IndexingError> successResult = Collections.emptyList();

        when(messagesAdapter.bulkIndex(any()))
                .thenReturn(errorResult)
                .thenReturn(successResult);

        final List<String> result = messages.bulkIndex(messagesWithIds("blocked-id", "other-error-id"));

        verify(messagesAdapter, times(2)).bulkIndex(any());
        assertThat(result).containsOnly("other-error-id");
    }

    @Test
    public void retriedIndexBlockErrorsThatFailWithDifferentErrorsAreTreatedAsPersistentFailures() throws IOException {
        final List<Messages.IndexingError> errorResult = ImmutableList.of(
                errorResultItem("blocked-id", Messages.IndexingError.ErrorType.IndexBlocked, "Index is read-only"),
                errorResultItem("other-error-id", Messages.IndexingError.ErrorType.IndexBlocked, "Index is read-only")
        );
        final List<Messages.IndexingError> secondErrorResult = ImmutableList.of(
                errorResultItem("other-error-id", Messages.IndexingError.ErrorType.Unknown, "Some other error")
        );

        when(messagesAdapter.bulkIndex(any()))
                .thenReturn(errorResult)
                .thenReturn(secondErrorResult);

        final List<String> result = messages.bulkIndex(messagesWithIds("blocked-id", "other-error-id"));

        verify(messagesAdapter, times(2)).bulkIndex(any());
        assertThat(result).containsOnly("other-error-id");
    }

    private List<Map.Entry<IndexSet, Message>> messagesWithIds(String... ids) {
        return Arrays.stream(ids)
                .map(this::messageWithId)
                .map(m -> new AbstractMap.SimpleEntry<>(mock(IndexSet.class), m))
                .collect(Collectors.toList());
    }

    private Message messageWithId(String id) {
        final Message mockedMessage = mock(Message.class);
        when(mockedMessage.getId()).thenReturn(id);
        when(mockedMessage.getTimestamp()).thenReturn(DateTime.now(DateTimeZone.UTC));
        return mockedMessage;
    }

    private List<Map.Entry<IndexSet, Message>> messageListWith(Message mockedMessage) {
        return ImmutableList.of(
                new AbstractMap.SimpleEntry<>(mock(IndexSet.class), mockedMessage)
        );
    }

    private Messages.IndexingError errorResultItem(String messageId, Messages.IndexingError.ErrorType errorType, String errorReason) {
        final Message message = mock(Message.class);
        when(message.getTimestamp()).thenReturn(DateTime.now(DateTimeZone.UTC));
        when(message.getId()).thenReturn(messageId);

        return Messages.IndexingError.create(message, "randomIndex", errorType, errorReason);
    }
}
