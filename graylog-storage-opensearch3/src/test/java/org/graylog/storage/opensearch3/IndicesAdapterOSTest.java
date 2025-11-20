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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.storage.opensearch3.cluster.ClusterStateApi;
import org.graylog.storage.opensearch3.indextemplates.OSSerializationUtils;
import org.graylog.storage.opensearch3.stats.ClusterStatsApi;
import org.graylog.storage.opensearch3.stats.IndexStatisticsBuilder;
import org.graylog.storage.opensearch3.stats.StatsApi;
import org.graylog2.indexer.indices.IndexTemplateAdapter;
import org.graylog2.indexer.indices.stats.IndexStatistics;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FlushStats;
import org.opensearch.client.opensearch.cat.OpenSearchCatClient;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.opensearch.client.opensearch.indices.stats.IndexStats;
import org.opensearch.client.opensearch.indices.stats.IndicesStats;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndicesAdapterOSTest {

    private IndicesAdapterOS toTest;

    @Mock
    private OfficialOpensearchClient opensearchClient;
    @Mock
    private StatsApi statsApi;
    @Mock
    private ClusterStatsApi clusterStatsApi;
    @Mock
    private ClusterStateApi clusterStateApi;
    @Mock
    private IndexTemplateAdapter indexTemplateAdapter;
    @Mock
    private IndexStatisticsBuilder indexStatisticsBuilder;
    @Mock
    private PlainJsonApi jsonApi;

    @BeforeEach
    void setUp() {
        OpenSearchClient client = mock(OpenSearchClient.class);
        when(opensearchClient.sync()).thenReturn(client);
        when(client.indices()).thenReturn(mock(OpenSearchIndicesClient.class));
        when(client.cat()).thenReturn(mock(OpenSearchCatClient.class));
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        toTest = new IndicesAdapterOS(
                opensearchClient,
                statsApi,
                clusterStatsApi,
                clusterStateApi,
                indexTemplateAdapter,
                indexStatisticsBuilder,
                objectMapper,
                jsonApi,
                new OSSerializationUtils()
        );
    }

    @Test
    void testIndicesStats() {
        IndicesStats stats1 = mock(IndicesStats.class);
        IndicesStats stats2 = mock(IndicesStats.class);
        IndexStatistics builtStats1 = mock(IndexStatistics.class);
        IndexStatistics builtStats2 = mock(IndexStatistics.class);
        doReturn(builtStats1).when(indexStatisticsBuilder).build("index_1", stats1);
        doReturn(builtStats2).when(indexStatisticsBuilder).build("index_2", stats2);

        doReturn(Map.of("index_1", stats1, "index_2", stats2)).when(statsApi).indicesStatsWithShardLevel(List.of("index_1", "index_2"));
        assertEquals(Set.of(builtStats1, builtStats2), toTest.indicesStats(List.of("index_1", "index_2")));

        doReturn(Map.of()).when(statsApi).indicesStatsWithShardLevel(List.of("index_3333"));
        assertEquals(Set.of(), toTest.indicesStats(List.of("index_3333")));
    }

    @Test
    void testIndexStats() {
        IndicesStats stats = mock(IndicesStats.class);
        IndexStatistics builtStats = mock(IndexStatistics.class);
        doReturn(builtStats).when(indexStatisticsBuilder).build("index_1", stats);

        doReturn(stats).when(statsApi).indexStatsWithShardLevel("index_1");
        assertEquals(Optional.of(builtStats), toTest.getIndexStats("index_1"));

        doReturn(null).when(statsApi).indexStatsWithShardLevel("index_3333");
        assertTrue(toTest.getIndexStats("index_3333").isEmpty());
    }

    @Test
    void testStoreSizeInBytes() {
        doReturn(Optional.of(42L)).when(statsApi).storeSizes("index_1");
        doReturn(Optional.of(0L)).when(statsApi).storeSizes("index_2");

        assertEquals(Optional.of(42L), toTest.storeSizeInBytes("index_1"));
        assertEquals(Optional.of(0L), toTest.storeSizeInBytes("index_2"));
        assertEquals(Optional.empty(), toTest.storeSizeInBytes("index_3333"));
    }

    @Test
    void testGetIndexStatsAsJsonNode() {
        final IndicesStats graylog13Stats = buildIndicesStats(
                "P999CCTTFOhEGdspOYh9Q",
                13L,
                FlushStats.builder().total(333L).totalTimeInMillis(1000L).periodic(676L)
        );
        final IndicesStats graylog12Stats = buildIndicesStats(
                "P999CCTTFOhEGdspOYh9V",
                17L,
                FlushStats.builder().total(1L).totalTimeInMillis(0L).periodic(1L)
        );

        final LinkedHashMap<String, IndicesStats> returnedMap = new LinkedHashMap<>();
        returnedMap.put("graylog_13", graylog13Stats);
        returnedMap.put("graylog_12", graylog12Stats);
        doReturn(returnedMap)
                .when(statsApi)
                .indicesStatsWithDocsAndStore(List.of("graylog_13", "graylog_12"));


        String expected = """
                {
                  "graylog_13": {
                    "primaries": {
                      "docs": {
                        "count": 13
                      },
                      "flush": {
                        "periodic": 676,
                        "total": 333,
                        "total_time_in_millis": 1000
                      }
                    },
                    "total": {},
                    "uuid": "P999CCTTFOhEGdspOYh9Q"
                  },
                  "graylog_12": {
                    "primaries": {
                      "docs": {
                        "count": 17
                      },
                      "flush": {
                        "periodic": 1,
                        "total": 1,
                        "total_time_in_millis": 0
                      }
                    },
                    "total": {},
                    "uuid": "P999CCTTFOhEGdspOYh9V"
                  }
                }
                """.replaceAll("\\s+", "").trim();
        final JsonNode returned = toTest.getIndexStats(List.of("graylog_13", "graylog_12"));

        assertEquals(expected, returned.toString());
    }


    private IndicesStats buildIndicesStats(final String uuid, final long count, final FlushStats.Builder flushStatsBuilder) {
        return IndicesStats.builder()
                .uuid(uuid)
                .primaries(IndexStats.builder()
                        .docs(builder -> builder.count(count))
                        .flush(flushStatsBuilder.build())
                        .build())
                .total(IndexStats.builder().build())
                .build();
    }


}
