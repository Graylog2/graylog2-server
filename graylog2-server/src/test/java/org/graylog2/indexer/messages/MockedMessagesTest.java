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

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.searchbox.client.JestClient;
import io.searchbox.core.BulkResult;
import org.graylog2.indexer.IndexSet;
import org.graylog2.plugin.Message;
import org.graylog2.system.processing.InMemoryProcessingStatusRecorder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.indexer.messages.Messages.INDEX_BLOCK_ERROR;
import static org.graylog2.indexer.messages.Messages.INDEX_BLOCK_REASON;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MockedMessagesTest {
    public static class MockedBulkResult extends BulkResult {
        MockedBulkResult() {
            super((ObjectMapper) null);
        }

        class MockedBulkResultItem extends BulkResult.BulkResultItem {
            MockedBulkResultItem(String operation, String index, String type, String id, int status, String error, Integer version, String errorType, String errorReason) {
                super(operation, index, type, id, status, error, version, errorType, errorReason);
            }
        }

        @SuppressWarnings("SameParameterValue")
        MockedBulkResultItem createResultItem(String operation, String index, String type, String id, int status, String error, Integer version, String errorType, String errorReason) {
            return new MockedBulkResultItem(operation, index, type, id, status, error, version, errorType, errorReason);
        }
    }

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private JestClient jestClient;
    private Messages messages;

    @Before
    public void setUp() throws Exception {
        this.messages = new Messages(new MetricRegistry(), jestClient, new InMemoryProcessingStatusRecorder(), true);
    }

    @Test
    public void bulkIndexingShouldNotDoAnythingForEmptyList() throws Exception {
        final List<String> result = messages.bulkIndex(Collections.emptyList());

        assertThat(result).isNotNull()
                .isEmpty();

        verify(jestClient, never()).execute(any());
    }

    @Test
    public void bulkIndexingShouldRetry() throws Exception {
        final BulkResult jestResult = mock(BulkResult.class);
        when(jestResult.isSucceeded()).thenReturn(true);
        when(jestResult.getFailedItems()).thenReturn(Collections.emptyList());

        when(jestClient.execute(any()))
                .thenThrow(new IOException("Boom!"))
                .thenReturn(jestResult);

        final List<Map.Entry<IndexSet, Message>> messageList = messageListWith(mock(Message.class));
        final List<String> result = messages.bulkIndex(messageList);

        assertThat(result).isNotNull().isEmpty();

        verify(jestClient, times(2)).execute(any());
    }

    @Test
    public void bulkIndexingShouldNotRetryForIndexMappingErrors() throws Exception {
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

        final List<Map.Entry<IndexSet, Message>> messageList = messageListWith(messageWithId(messageId));

        final List<String> result = messages.bulkIndex(messageList);

        assertThat(result).hasSize(1);

        verify(jestClient, times(1)).execute(any());
    }

    @Test
    public void bulkIndexingShouldRetryIfIndexBlocked() throws IOException {
        final BulkResult errorResult = mock(BulkResult.class);
        final BulkResult.BulkResultItem errorItem = errorResultItem("blocked-id", INDEX_BLOCK_ERROR, INDEX_BLOCK_REASON);
        when(errorResult.isSucceeded()).thenReturn(false);
        when(errorResult.getFailedItems()).thenReturn(ImmutableList.of(errorItem));

        final BulkResult successResult = mock(BulkResult.class);

        when(jestClient.execute(any()))
                .thenReturn(errorResult)
                .thenReturn(successResult);

        final List<String> result = messages.bulkIndex(messagesWithIds("blocked-id"));

        verify(jestClient, times(2)).execute(any());
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    public void indexBlockedRetriesShouldOnlyRetryIndexBlockedErrors() throws IOException {
        final BulkResult errorResult = mock(BulkResult.class);
        final BulkResult.BulkResultItem indexBlockedError = errorResultItem("blocked-id", INDEX_BLOCK_ERROR, INDEX_BLOCK_REASON);
        final BulkResult.BulkResultItem otherError = errorResultItem("other-error-id", "something else", "something else");
        when(errorResult.getFailedItems()).thenReturn(ImmutableList.of(indexBlockedError, otherError));

        final BulkResult successResult = mock(BulkResult.class);

        when(jestClient.execute(any()))
                .thenReturn(errorResult)
                .thenReturn(successResult);

        final List<String> result = messages.bulkIndex(messagesWithIds("blocked-id", "other-error-id"));

        verify(jestClient, times(2)).execute(any());
        assertThat(result).containsOnly("other-error-id");
    }

    @Test
    public void retriedIndexBlockErrorsThatFailWithDifferentErrorsAreTreatedAsPersistentFailures() throws IOException {
        final BulkResult errorResult = mock(BulkResult.class);
        final BulkResult.BulkResultItem indexBlockedError = errorResultItem("blocked-id", INDEX_BLOCK_ERROR, INDEX_BLOCK_REASON);
        final BulkResult.BulkResultItem initialIndexBlockedError = errorResultItem("other-error-id", INDEX_BLOCK_ERROR, INDEX_BLOCK_REASON);
        when(errorResult.getFailedItems()).thenReturn(ImmutableList.of(indexBlockedError, initialIndexBlockedError));

        final BulkResult secondErrorResult = mock(BulkResult.class);
        final BulkResult.BulkResultItem otherError = errorResultItem("other-error-id", "something else", "something else");
        when(secondErrorResult.getFailedItems()).thenReturn(ImmutableList.of(otherError));

        when(jestClient.execute(any()))
                .thenReturn(errorResult)
                .thenReturn(secondErrorResult);

        final List<String> result = messages.bulkIndex(messagesWithIds("blocked-id", "other-error-id"));

        verify(jestClient, times(2)).execute(any());
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
