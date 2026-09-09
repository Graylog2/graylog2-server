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
package org.graylog.storage.opensearch3;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.storage.opensearch3.testing.client.mock.ServerlessOpenSearchClient;
import org.graylog2.indexer.IncompleteBulkResponseException;
import org.graylog2.indexer.messages.ChunkedBulkIndexer;
import org.graylog2.indexer.messages.Indexable;
import org.graylog2.indexer.messages.IndexingRequest;
import org.graylog2.indexer.results.ResultMessageFactory;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.util.MissingRequiredPropertyException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessagesAdapterOSTest {

    @Mock
    private ResultMessageFactory resultMessageFactory;

    /**
     * Reproduces a rolling restart where a replica shard is still being promoted: OpenSearch reports a shard-level
     * write failure whose {@code _shards.failures[]} entry omits the "shard" number. The opensearch-java client
     * treats that as a required property and refuses to parse the response at all.
     */
    private static final String BULK_RESPONSE_WITH_INCOMPLETE_SHARD_FAILURE = """
            {
              "took": 1,
              "errors": true,
              "items": [
                {
                  "index": {
                    "_index": "graylog_0",
                    "_id": "message-id",
                    "status": 200,
                    "_shards": {
                      "total": 2,
                      "successful": 1,
                      "failed": 1,
                      "failures": [
                        {
                          "index": "graylog_0",
                          "node": "node-1",
                          "reason": {
                            "type": "unavailable_shards_exception",
                            "reason": "primary shard is not active"
                          }
                        }
                      ]
                    }
                  }
                }
              ]
            }
            """;

    /**
     * A missing "status" is a required-property failure too, but it has nothing to do with shard reallocation.
     * It must not be mistaken for the scenario above and silently retried forever.
     */
    private static final String BULK_RESPONSE_MISSING_UNRELATED_REQUIRED_PROPERTY = """
            {
              "took": 1,
              "errors": true,
              "items": [
                {
                  "index": {
                    "_index": "graylog_0",
                    "_id": "message-id"
                  }
                }
              ]
            }
            """;

    @Test
    void bulkIndexTreatsIncompleteShardFailureResponseAsRetryable() {
        final OfficialOpensearchClient officialOpensearchClient = ServerlessOpenSearchClient.builder()
                .stubResponse("POST", "/_bulk", BULK_RESPONSE_WITH_INCOMPLETE_SHARD_FAILURE)
                .build();

        final MessagesAdapterOS messagesAdapterOS = new MessagesAdapterOS(resultMessageFactory, officialOpensearchClient,
                new MetricRegistry(), new ChunkedBulkIndexer(), new ObjectMapper());

        final Indexable message = mock(Indexable.class);
        when(message.getId()).thenReturn("message-id");
        when(message.toElasticSearchObject(any(), any())).thenReturn(Map.of("message", "test"));

        final List<IndexingRequest> request = List.of(IndexingRequest.create("graylog_0", message));

        assertThatThrownBy(() -> messagesAdapterOS.bulkIndex(request))
                .isInstanceOf(IncompleteBulkResponseException.class)
                .satisfies(e -> assertThat(ExceptionUtils.hasCauseOf(e, MissingRequiredPropertyException.class)).isTrue());
    }

    @Test
    void bulkIndexDoesNotTreatUnrelatedMissingPropertyAsRetryable() {
        final OfficialOpensearchClient officialOpensearchClient = ServerlessOpenSearchClient.builder()
                .stubResponse("POST", "/_bulk", BULK_RESPONSE_MISSING_UNRELATED_REQUIRED_PROPERTY)
                .build();

        final MessagesAdapterOS messagesAdapterOS = new MessagesAdapterOS(resultMessageFactory, officialOpensearchClient,
                new MetricRegistry(), new ChunkedBulkIndexer(), new ObjectMapper());

        final Indexable message = mock(Indexable.class);
        when(message.getId()).thenReturn("message-id");
        when(message.toElasticSearchObject(any(), any())).thenReturn(Map.of("message", "test"));

        final List<IndexingRequest> request = List.of(IndexingRequest.create("graylog_0", message));

        // Must propagate as the original, unwrapped exception rather than being turned into a retryable one.
        assertThatThrownBy(() -> messagesAdapterOS.bulkIndex(request))
                .isInstanceOf(MissingRequiredPropertyException.class)
                .hasMessageContaining("status");
    }
}
