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
package org.graylog.storage.opensearch3.stats;

import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchAsyncClient;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.ShardStatistics;
import org.opensearch.client.opensearch.indices.IndicesStatsRequest;
import org.opensearch.client.opensearch.indices.IndicesStatsResponse;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.opensearch.client.opensearch.indices.stats.AllIndicesStats;
import org.opensearch.client.opensearch.indices.stats.IndexStats;
import org.opensearch.client.opensearch.indices.stats.IndicesStats;
import org.opensearch.client.opensearch.indices.stats.IndicesStatsMetric;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class StatsApiTest {

    private StatsApi statsApi;
    @Mock
    private OpenSearchClient syncClient;
    @Mock
    private OpenSearchAsyncClient asyncClient;
    @Mock
    private OpenSearchIndicesClient indicesClient;


    @BeforeEach
    void setUp() {
        doReturn(indicesClient).when(syncClient).indices();
        final OfficialOpensearchClient client = new OfficialOpensearchClient(syncClient, asyncClient);
        statsApi = new StatsApi(client);
    }

    @Test
    void testStoreSizes() throws Exception {
        final IndicesStatsResponse indicesStatsResponse = IndicesStatsResponse.builder()
                .all(
                        AllIndicesStats.builder()
                                .primaries(
                                        IndexStats.builder()
                                                .docs(builder -> builder.count(13L))
                                                .build()
                                )
                                .total(emptyIndexStats())
                                .build()
                )
                .indices(Map.of("graylog_existing",
                        IndicesStats.builder()
                                .uuid("P999CCTTFOhEGdspOYh9Q")
                                .primaries(IndexStats.builder()
                                        .docs(builder -> builder.count(13L))
                                        .store(builder -> builder.sizeInBytes(123456L).reservedInBytes(42L))
                                        .build())
                                .total(emptyIndexStats())
                                .build()))
                .shards(mock(ShardStatistics.class))
                .build();
        doReturn(indicesStatsResponse)
                .when(indicesClient)
                .stats(IndicesStatsRequest.builder().index(List.of("graylog_existing")).metric(List.of(IndicesStatsMetric.Store)).build());
        doReturn(indicesStatsResponse)
                .when(indicesClient)
                .stats(IndicesStatsRequest.builder().index(List.of("graylog_missing")).metric(List.of(IndicesStatsMetric.Store)).build());

        assertEquals(Optional.of(123456L), statsApi.storeSizes("graylog_existing"));
        assertEquals(Optional.empty(), statsApi.storeSizes("graylog_missing"));
    }

    @Test
    void testIndicesStatsOnMissingIndex() throws Exception {
        final IndicesStatsResponse indicesStatsResponse = IndicesStatsResponse.builder()
                .all(
                        AllIndicesStats.builder()
                                .primaries(emptyIndexStats())
                                .total(emptyIndexStats())
                                .build()
                )
                .indices(Map.of())
                .shards(mock(ShardStatistics.class))
                .build();

        doReturn(indicesStatsResponse)
                .when(indicesClient)
                .stats(
                        argThat((ArgumentMatcher<IndicesStatsRequest>) argument -> List.of("graylog_missing").equals(argument.index()))
                );

        assertTrue(statsApi.indicesStatsWithDocsAndStore(List.of("graylog_missing")).isEmpty());
        assertTrue(statsApi.indicesStatsWithShardLevel(List.of("graylog_missing")).isEmpty());
        assertNull(statsApi.indexStatsWithShardLevel("graylog_missing"));
    }

    @Test
    void testIndicesStatsReturnProperStats() throws Exception {
        final IndicesStats graylog13Stats = buildIndicesStats("P999CCTTFOhEGdspOYh9Q", 13L);
        final IndicesStats graylog14Stats = buildIndicesStats("P999CCTTFOhEGdspOYh9V", 17L);
        final IndicesStatsResponse multipleIndicesResponse = IndicesStatsResponse.builder()
                .all(
                        mock(AllIndicesStats.class)
                )
                .indices(
                        Map.of("graylog_13", graylog13Stats, "graylog_12", graylog14Stats)
                )
                .shards(
                        mock(ShardStatistics.class)
                )
                .build();

        doReturn(multipleIndicesResponse)
                .when(indicesClient)
                .stats(any(IndicesStatsRequest.class));

        assertEquals(Map.of("graylog_13", graylog13Stats, "graylog_12", graylog14Stats),
                statsApi.indicesStatsWithShardLevel(List.of("graylog_13", "graylog_14")));
        assertEquals(Map.of("graylog_13", graylog13Stats, "graylog_12", graylog14Stats),
                statsApi.indicesStatsWithDocsAndStore(List.of("graylog_13", "graylog_14")));
        assertEquals(graylog13Stats, statsApi.indexStatsWithShardLevel("graylog_13"));
        assertEquals(graylog14Stats, statsApi.indexStatsWithShardLevel("graylog_12"));
        assertNull(statsApi.indexStatsWithShardLevel("graylog_missing"));
    }

    private IndicesStats buildIndicesStats(final String uuid, final long count) {
        return IndicesStats.builder()
                .uuid(uuid)
                .primaries(IndexStats.builder()
                        .docs(builder -> builder.count(count))
                        .build())
                .total(emptyIndexStats())
                .build();
    }


    private IndexStats emptyIndexStats() {
        return IndexStats.builder().build();
    }
}
