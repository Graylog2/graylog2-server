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
package org.graylog.storage.elasticsearch6;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.searchbox.client.JestClient;
import io.searchbox.core.BulkResult;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.messages.ChunkedBulkIndexer;
import org.graylog2.indexer.messages.IndexingRequest;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.plugin.Message;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.graylog.storage.elasticsearch6.MessagesAdapterES6.INDEX_BLOCK_ERROR;
import static org.graylog.storage.elasticsearch6.MessagesAdapterES6.INDEX_BLOCK_REASON;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessagesAdapterES6Test {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private MessagesAdapterES6 messagesAdapter;
    private JestClient jestClient;

    @BeforeEach
    void setUp() {
        this.jestClient = mock(JestClient.class);
        final MetricRegistry metricRegistry = mock(MetricRegistry.class);
        this.messagesAdapter = new MessagesAdapterES6(jestClient, true, metricRegistry, new ChunkedBulkIndexer(), objectMapper);
    }

    public static class MockedBulkResult extends BulkResult {
        MockedBulkResult() {
            super((ObjectMapper)null);
        }

        class MockedBulkResultItem extends BulkResult.BulkResultItem {
            MockedBulkResultItem(String operation, String index, String type, String id, int status, String error, Integer version, String errorType, String errorReason) {
                super(operation, index, type, id, status, error, version, errorType, errorReason);
            }
        }

        MockedBulkResultItem createResultItem(String operation, String index, String type, String id, int status, String error, Integer version, String errorType, String errorReason) {
            return new MockedBulkResultItem(operation, index, type, id, status, error, version, errorType, errorReason);
        }
    }

    @Test
    public void bulkIndexingShouldNotDoAnythingForEmptyList() throws Exception {
        final List<Messages.IndexingError> result = messagesAdapter.bulkIndex(Collections.emptyList());

        assertThat(result).isNotNull()
                .isEmpty();

        verify(jestClient, never()).execute(any());
    }

    @Test
    void bulkIndexReturnsEmptyListIfSuccessful() throws Exception {
        final BulkResult jestResult = mock(BulkResult.class);
        when(jestResult.isSucceeded()).thenReturn(true);
        when(jestClient.execute(any())).thenReturn(jestResult);
        final List<IndexingRequest> messageList = messageListWith(messageWithId("some-message"));

        final List<Messages.IndexingError> indexingErrors = messagesAdapter.bulkIndex(messageList);

        assertThat(indexingErrors).isEmpty();
    }

    @Test
    public void bulkIndexingParsesIndexMappingErrors() throws Exception {
        final String messageId = "BOOMID";

        final BulkResult jestResult = mock(BulkResult.class);
        final BulkResult.BulkResultItem bulkResultItem = new MockedBulkResult().createResultItem(
                "index",
                "someindex",
                "message",
                messageId,
                400,
                "{\"type\":\"mapper_parsing_exception\",\"reason\":\"failed to parse [http_response_code]\",\"caused_by\":{\"type\":\"number_format_exception\",\"reason\":\"For input string: \\\"FOOBAR\\\"\"}}",
                null,
                "mapper_parsing_exception",
                "failed to parse [http_response_code]"
        );
        when(jestResult.isSucceeded()).thenReturn(false);
        when(jestResult.getFailedItems()).thenReturn(ImmutableList.of(bulkResultItem));

        when(jestClient.execute(any()))
            .thenReturn(jestResult)
            .thenThrow(new IllegalStateException("JestResult#execute should not be called twice."));

        final List<IndexingRequest> messageList = messageListWith(messageWithId(messageId));

        final List<Messages.IndexingError> result = messagesAdapter.bulkIndex(messageList);

        assertThat(result).hasSize(1)
                .extracting(indexingError -> indexingError.message().getId(), Messages.IndexingError::errorType, Messages.IndexingError::errorMessage)
                .containsExactly(tuple(messageId, Messages.IndexingError.ErrorType.MappingError, "failed to parse [http_response_code]"));
    }

    @Test
    public void bulkIndexingParsesPrimaryShardUnavailableErrors() throws Exception {
        final String messageId = "BOOMID";

        final BulkResult failedJestResult = mock(BulkResult.class);
        final BulkResult.BulkResultItem bulkResultItem = new MockedBulkResult().createResultItem(
                "index",
                "someindex",
                "message",
                messageId,
                400,
                "{\"type\":\"unavailable_shards_exception\",\"reason\":\"primary shard is not active\"\"}}",
                null,
                "unavailable_shards_exception",
                "primary shard is not active"
        );
        when(failedJestResult.isSucceeded()).thenReturn(false);
        when(failedJestResult.getFailedItems()).thenReturn(ImmutableList.of(bulkResultItem));

        when(jestClient.execute(any()))
            .thenReturn(failedJestResult)
            .thenThrow(new IllegalStateException("JestResult#execute should not be called twice."));

        final List<IndexingRequest> messageList = messageListWith(messageWithId(messageId));

        final List<Messages.IndexingError> result = messagesAdapter.bulkIndex(messageList);

        assertThat(result).hasSize(1)
                .extracting(indexingError -> indexingError.message().getId(), Messages.IndexingError::errorType, Messages.IndexingError::errorMessage)
                .containsExactly(tuple(messageId, Messages.IndexingError.ErrorType.IndexBlocked, "primary shard is not active"));
    }

    @Test
    public void bulkIndexPropagatesIOExceptions() throws Exception {
        when(jestClient.execute(any()))
                .thenThrow(new IOException("Boom!"));

        final List<IndexingRequest> messageList = messageListWith(mock(Message.class));

        assertThatThrownBy(() -> messagesAdapter.bulkIndex(messageList))
                .isInstanceOf(IOException.class);
    }

    @Test
    public void parsesErrorTypesAndReturnsIndexingErrors() throws IOException {
        final BulkResult errorResult = mock(BulkResult.class);
        final BulkResult.BulkResultItem indexBlockedError = errorResultItem("blocked-id", INDEX_BLOCK_ERROR, INDEX_BLOCK_REASON);
        final BulkResult.BulkResultItem otherError = errorResultItem("other-error-id", "something else", "something else");
        when(errorResult.getFailedItems()).thenReturn(ImmutableList.of(indexBlockedError, otherError));

        when(jestClient.execute(any()))
                .thenReturn(errorResult);

        final List<Messages.IndexingError> result = messagesAdapter.bulkIndex(messagesWithIds("blocked-id", "other-error-id"));

        verify(jestClient, times(1)).execute(any());
        assertThat(result).extracting(indexingError -> indexingError.message().getId(), Messages.IndexingError::errorType, Messages.IndexingError::errorMessage)
                .containsExactlyInAnyOrder(
                        tuple("blocked-id", Messages.IndexingError.ErrorType.IndexBlocked, INDEX_BLOCK_REASON),
                        tuple("other-error-id", Messages.IndexingError.ErrorType.Unknown, "something else")
                );
    }
    private List<IndexingRequest> messagesWithIds(String... ids) {
        return Arrays.stream(ids)
                .map(this::messageWithId)
                .map(m -> IndexingRequest.create(mock(IndexSet.class), m))
                .collect(Collectors.toList());
    }

    private Message messageWithId(String id) {
        final Message mockedMessage = mock(Message.class);
        when(mockedMessage.getId()).thenReturn(id);
        when(mockedMessage.getTimestamp()).thenReturn(DateTime.now(DateTimeZone.UTC));
        return mockedMessage;
    }

    private List<IndexingRequest> messageListWith(Message mockedMessage) {
        return ImmutableList.of(
                IndexingRequest.create(mock(IndexSet.class), mockedMessage)
        );
    }

    private BulkResult.BulkResultItem errorResultItem(String messageId, String errorType, String errorReason) {
        return new MockedBulkResult().createResultItem(
                "index",
                "someindex",
                "message",
                messageId,
                400,
                "{\"type\":\"" + errorType + "\",\"reason\":\"" + errorReason + "\"}}",
                null,
                errorType,
                errorReason
        );
    }
}
