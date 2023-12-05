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

import org.graylog.failure.FailureSubmissionService;
import org.graylog2.Configuration;
import org.graylog2.indexer.IndexSet;
import org.graylog2.plugin.Message;
import org.graylog2.system.processing.ProcessingStatusRecorder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.indexer.messages.IndexingError.Type.IndexBlocked;
import static org.graylog2.indexer.messages.IndexingError.Type.MappingError;
import static org.graylog2.indexer.messages.IndexingError.Type.Unknown;
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
    private final Configuration conf = mock(Configuration.class);

    private Messages messages;

    @BeforeEach
    void setUp() {
        when(conf.getFailureHandlingQueueCapacity()).thenReturn(1000);
        this.messages = new Messages(trafficAccounting, messagesAdapter, processingStatusRecorder,
                mock(FailureSubmissionService.class));
    }

    @Test
    public void bulkIndexingShouldNotDoAnythingForEmptyList() throws Exception {
        final IndexingResults indexingResults = messages.bulkIndex(Collections.emptyList());

        assertThat(indexingResults).isNotNull();
        assertThat(indexingResults.allResults()).isEmpty();

        verify(messagesAdapter, never()).bulkIndex(any());
    }

    @Test
    public void bulkIndexingShouldNotRetryForIndexMappingErrors() throws Exception {
        final String messageId = "BOOMID";

        final IndexingResults errorResult =
                IndexingResults.create(List.of(), List.of(errorResultItem(messageId, MappingError, "failed to parse [http_response_code]")));

        when(messagesAdapter.bulkIndex(any()))
                .thenReturn(errorResult)
                .thenThrow(new IllegalStateException("JestResult#execute should not be called twice."));

        final Message mockedMessage = mock(Message.class);
        when(mockedMessage.getId()).thenReturn(messageId);
        when(mockedMessage.getTimestamp()).thenReturn(DateTime.now(DateTimeZone.UTC));

        final List<MessageWithIndex> messageList = messageListWith(mockedMessage);

        var result = messages.bulkIndex(messageList);

        assertThat(result.errors()).hasSize(1);
        assertThat(result.successes()).hasSize(0);

        verify(messagesAdapter, times(1)).bulkIndex(any());
    }

    @Test
    public void bulkIndexingShouldRetry() throws Exception {
        when(messagesAdapter.bulkIndex(any()))
                .thenThrow(new IOException("Boom!"))
                .thenReturn(IndexingResults.empty());

        final List<MessageWithIndex> messageList = messageListWith(mock(Message.class));

        var result = messages.bulkIndex(messageList);

        assertThat(result).isNotNull();
        assertThat(result.allResults()).isEmpty();

        verify(messagesAdapter, times(2)).bulkIndex(any());
    }

    @Test
    public void bulkIndexingShouldRetryIfIndexBlocked() throws IOException {
        final IndexingResults errorResult =
                IndexingResults.create(List.of(), List.of(errorResultItem("blocked-id", IndexBlocked, "Index is read-only")));

        when(messagesAdapter.bulkIndex(any()))
                .thenReturn(errorResult)
                .thenReturn(IndexingResults.empty());

        var result = messages.bulkIndex(messagesWithIds("blocked-id"));

        verify(messagesAdapter, times(2)).bulkIndex(any());
        assertThat(result).isNotNull();
        assertThat(result.allResults()).isEmpty();
    }

    @Test
    public void indexBlockedRetriesShouldOnlyRetryIndexBlockedErrors() throws IOException {
        final IndexingResults errorResult =
                IndexingResults.create(List.of(),
                        List.of(
                                errorResultItem("blocked-id", IndexBlocked, "Index is read-only"),
                                errorResultItem("other-error-id", Unknown, "Some other error")
                        )
                );

        when(messagesAdapter.bulkIndex(any()))
                .thenReturn(errorResult)
                .thenReturn(IndexingResults.empty());

        var result = messages.bulkIndex(messagesWithIds("blocked-id", "other-error-id"));

        verify(messagesAdapter, times(2)).bulkIndex(any());
        assertThat(result.errors()).map(IndexingError::message).map(Indexable::getId)
                .containsOnly("other-error-id");
    }

    @Test
    public void retriedIndexBlockErrorsThatFailWithDifferentErrorsAreTreatedAsPersistentFailures() throws IOException {
        final IndexingError someOtherError = errorResultItem("other-error-id", Unknown, "Some other error");
        final IndexingResults errorResult =
                IndexingResults.create(List.of(),
                        List.of(
                                errorResultItem("blocked-id", IndexBlocked, "Index is read-only"),
                                someOtherError
                        )
                );
        final IndexingResults secondErrorResult =
                IndexingResults.create(List.of(),
                        List.of(
                                someOtherError
                        )
                );

        when(messagesAdapter.bulkIndex(any()))
                .thenReturn(errorResult)
                .thenReturn(secondErrorResult);

        var result = messages.bulkIndex(messagesWithIds("blocked-id", "other-error-id"));

        verify(messagesAdapter, times(2)).bulkIndex(any());
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors()).map(IndexingError::message).map(Indexable::getId).containsOnly("other-error-id");
    }

    private List<MessageWithIndex> messagesWithIds(String... ids) {
        return Arrays.stream(ids)
                .map(this::messageWithId)
                .map(m -> new MessageWithIndex(m, mock(IndexSet.class)))
                .collect(Collectors.toList());
    }

    private Message messageWithId(String id) {
        final Message mockedMessage = mock(Message.class);
        when(mockedMessage.getId()).thenReturn(id);
        when(mockedMessage.getTimestamp()).thenReturn(DateTime.now(DateTimeZone.UTC));
        return mockedMessage;
    }

    private List<MessageWithIndex> messageListWith(Message mockedMessage) {
        return List.of(new MessageWithIndex(mockedMessage, mock(IndexSet.class)));
    }

    private IndexingError errorResultItem(String messageId, IndexingError.Type errorType, String errorReason) {
        final Message message = mock(Message.class);
        when(message.getTimestamp()).thenReturn(DateTime.now(DateTimeZone.UTC));
        when(message.getId()).thenReturn(messageId);

        return IndexingError.create(message, "randomIndex", errorType, errorReason);
    }
}
