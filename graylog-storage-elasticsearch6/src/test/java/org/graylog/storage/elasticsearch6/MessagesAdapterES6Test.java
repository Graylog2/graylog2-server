package org.graylog.storage.elasticsearch6;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.searchbox.client.JestClient;
import io.searchbox.core.BulkResult;
import org.graylog2.indexer.IndexFailure;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.messages.IndexingRequest;
import org.graylog2.plugin.Message;
import org.graylog2.system.processing.ProcessingStatusRecorder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessagesAdapterES6Test {
    private MessagesAdapterES6 messagesAdapter;
    private JestClient jestClient;
    private MetricRegistry metricRegistry;
    private ProcessingStatusRecorder processingStatusRecorder;

    @BeforeEach
    void setUp() {
        this.jestClient = mock(JestClient.class);
        this.metricRegistry = mock(MetricRegistry.class);
        this.processingStatusRecorder = mock(ProcessingStatusRecorder.class);
        this.messagesAdapter = new MessagesAdapterES6(jestClient, true, metricRegistry, processingStatusRecorder);
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
        final List<IndexFailure> result = messagesAdapter.bulkIndex(Collections.emptyList(), (successes) -> {});

        assertThat(result).isNotNull()
                .isEmpty();

        verify(jestClient, never()).execute(any());
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

        final List<IndexingRequest> messageList = ImmutableList.of(
                IndexingRequest.create(mock(IndexSet.class), mockedMessage)
        );

        final List<IndexFailure> result = messagesAdapter.bulkIndex(messageList, (successes) -> {});

        assertThat(result).hasSize(1);

        verify(jestClient, times(1)).execute(any());
    }

    @Test
    public void bulkIndexingShouldRetry() throws Exception {
        final BulkResult jestResult = mock(BulkResult.class);
        when(jestResult.isSucceeded()).thenReturn(true);
        when(jestResult.getFailedItems()).thenReturn(Collections.emptyList());

        when(jestClient.execute(any()))
                .thenThrow(new IOException("Boom!"))
                .thenReturn(jestResult);

        final List<IndexingRequest> messageList = ImmutableList.of(
                IndexingRequest.create(mock(IndexSet.class), mock(Message.class))
        );
        final List<IndexFailure> result = messagesAdapter.bulkIndex(messageList, (successes) -> {});

        assertThat(result).isNotNull().isEmpty();

        verify(jestClient, times(2)).execute(any());
    }
}
