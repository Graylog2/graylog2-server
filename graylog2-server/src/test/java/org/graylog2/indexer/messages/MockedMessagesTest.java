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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MockedMessagesTest {
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

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private JestClient jestClient;
    private Messages messages;

    @Before
    public void setUp() throws Exception {
        this.messages = new Messages(new MetricRegistry(), jestClient);
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

        final List<Map.Entry<IndexSet, Message>> messageList = ImmutableList.of(
            new AbstractMap.SimpleEntry(mock(IndexSet.class), mock(Message.class))
        );
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

        final Message mockedMessage = mock(Message.class);
        when(mockedMessage.getId()).thenReturn(messageId);
        when(mockedMessage.getTimestamp()).thenReturn(DateTime.now(DateTimeZone.UTC));

        final List<Map.Entry<IndexSet, Message>> messageList = ImmutableList.of(
            new AbstractMap.SimpleEntry(mock(IndexSet.class), mockedMessage)
        );

        final List<String> result = messages.bulkIndex(messageList);

        assertThat(result).hasSize(1);

        verify(jestClient, times(1)).execute(any());
    }
}
