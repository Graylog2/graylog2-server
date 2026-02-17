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
package org.graylog.storage.opensearch2.stats;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.graylog.storage.opensearch2.OpenSearchClient;
import org.graylog2.indexer.indices.util.IndexNameBatching;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StatsApiTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private OpenSearchClient client;

    private StatsApi statsApi;

    @BeforeEach
    void setUp() {
        statsApi = new StatsApi(objectMapper, client);
    }

    @Test
    void singleBatchMakesSingleRequest() {
        final List<String> indices = List.of("graylog_0", "graylog_1");
        final ObjectNode response = buildStatsResponse(indices);

        doAnswer(invocation -> response).when(client).execute(any(), anyString());

        final JsonNode result = statsApi.indexStatsWithDocsAndStore(indices);

        assertThat(result.size()).isEqualTo(2);
        assertThat(result.has("graylog_0")).isTrue();
        assertThat(result.has("graylog_1")).isTrue();
        verify(client, times(1)).execute(any(), anyString());
    }

    @Test
    void multipleBatchesMergeResults() {
        // Generate enough long-named indices to force multiple batches
        final List<String> indices = IntStream.range(0, 80)
                .mapToObj(i -> "restored-archive-data-lake-68b002df50e89877d351cb83_" + (1758100000000L + i))
                .toList();

        // Verify these actually require multiple batches
        final List<List<String>> batches = IndexNameBatching.partitionByJoinedLength(
                indices, IndexNameBatching.MAX_INDICES_URL_LENGTH);
        assertThat(batches.size()).isGreaterThan(1);

        // Each call to client.execute returns a response for the batch's indices
        final List<ObjectNode> batchResponses = new ArrayList<>();
        for (final List<String> batch : batches) {
            batchResponses.add(buildStatsResponse(batch));
        }

        final var callCount = new int[]{0};
        doAnswer(invocation -> {
            final ObjectNode resp = batchResponses.get(callCount[0]);
            callCount[0]++;
            return resp;
        }).when(client).execute(any(), anyString());

        final JsonNode result = statsApi.indexStatsWithDocsAndStore(indices);

        // All indices should be present in the merged result
        assertThat(result.size()).isEqualTo(indices.size());
        for (final String index : indices) {
            assertThat(result.has(index)).isTrue();
        }

        // Should have made one request per batch
        verify(client, times(batches.size())).execute(any(), anyString());
    }

    @Test
    void shardLevelStatsBatchCorrectly() {
        final List<String> indices = IntStream.range(0, 80)
                .mapToObj(i -> "restored-archive-data-lake-68b002df50e89877d351cb83_" + (1758100000000L + i))
                .toList();

        final List<List<String>> batches = IndexNameBatching.partitionByJoinedLength(
                indices, IndexNameBatching.MAX_INDICES_URL_LENGTH);

        final List<ObjectNode> batchResponses = new ArrayList<>();
        for (final List<String> batch : batches) {
            batchResponses.add(buildStatsResponse(batch));
        }

        final var callCount = new int[]{0};
        doAnswer(invocation -> {
            final ObjectNode resp = batchResponses.get(callCount[0]);
            callCount[0]++;
            return resp;
        }).when(client).execute(any(), anyString());

        final JsonNode result = statsApi.indexStatsWithShardLevel(indices);

        assertThat(result.size()).isEqualTo(indices.size());
        verify(client, times(batches.size())).execute(any(), anyString());
    }

    private ObjectNode buildStatsResponse(List<String> indices) {
        final ObjectNode indicesNode = objectMapper.createObjectNode();
        for (final String index : indices) {
            final ObjectNode indexStats = objectMapper.createObjectNode();
            final ObjectNode primaries = objectMapper.createObjectNode();
            final ObjectNode docs = objectMapper.createObjectNode();
            docs.put("count", 100L);
            final ObjectNode store = objectMapper.createObjectNode();
            store.put("size_in_bytes", 1024L);
            primaries.set("docs", docs);
            primaries.set("store", store);
            indexStats.set("primaries", primaries);
            indicesNode.set(index, indexStats);
        }

        final ObjectNode response = objectMapper.createObjectNode();
        response.set("indices", indicesNode);
        return response;
    }
}
