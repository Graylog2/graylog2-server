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


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.graylog2.indexer.indices.stats.IndexStatistics;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opensearch.client.opensearch._types.FlushStats;
import org.opensearch.client.opensearch._types.GetStats;
import org.opensearch.client.opensearch._types.IndexingStats;
import org.opensearch.client.opensearch._types.MergesStats;
import org.opensearch.client.opensearch._types.RefreshStats;
import org.opensearch.client.opensearch._types.SearchStats;
import org.opensearch.client.opensearch._types.SegmentsStats;
import org.opensearch.client.opensearch.indices.stats.IndexShardStats;
import org.opensearch.client.opensearch.indices.stats.IndexStats;
import org.opensearch.client.opensearch.indices.stats.IndicesStats;
import org.opensearch.client.opensearch.indices.stats.ShardRouting;
import org.opensearch.client.opensearch.indices.stats.ShardRoutingState;

import java.util.List;
import java.util.Map;

import static org.graylog2.rest.models.system.indexer.responses.IndexStats.TimeAndTotalStats.create;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;


class IndexStatisticsBuilderTest {

    private IndexStatisticsBuilder toTest;

    private final ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();

    private static final long NVM = 0L;

    private static final String SAMPLE_STATS_RESPONSE = """
            {
                  "uuid": "Whatever",
                  "primaries": {
                    "docs": {
                      "count": 111,
                      "deleted": 42
                    },
                    "store": {
                      "size_in_bytes": 1234567,
                      "reserved_in_bytes": 0
                    },
                    "indexing": {
                      "index_total": 17,
                      "index_time_in_millis": 30000
                    },
                    "get": {
                      "total": 7,
                      "time_in_millis": 2222
                    },
                    "search": {
                      "open_contexts": 2,
                      "query_total": 11,
                      "query_time_in_millis": 3212,
                      "fetch_total": 22,
                      "fetch_time_in_millis": 5212
                    },
                    "merges": {
                      "total": 13,
                      "total_time_in_millis": 3600000
                    },
                    "refresh": {
                      "total": 1,
                      "total_time_in_millis": 2000
                    },
                    "flush": {
                      "total": 6,
                      "total_time_in_millis": 1111
                    },
                    "segments": {
                      "count": 12
                    }
                  },
                  "total": {
                    "docs": {
                      "count": 111,
                      "deleted": 42
                    },
                    "store": {
                      "size_in_bytes": 1234567,
                      "reserved_in_bytes": 0
                    },
                    "indexing": {
                      "index_total": 17,
                      "index_time_in_millis": 30000
                    },
                    "get": {
                      "total": 7,
                      "time_in_millis": 2222
                    },
                    "search": {
                      "open_contexts": 2,
                      "query_total": 11,
                      "query_time_in_millis": 3112,
                      "fetch_total": 22,
                      "fetch_time_in_millis": 5112
                    },
                    "refresh": {
                      "total": 1,
                      "total_time_in_millis": 4000
                    },
                    "flush": {
                      "total": 6,
                      "total_time_in_millis": 1111
                    },
                    "segments": {
                      "count": 12
                    }
                  },
                  "shards": {
                    "0": [
                      {
                        "routing": {
                          "state": "STARTED",
                          "primary": true,
                          "node": "boss",
                          "relocating_node": "underboss"
                        }
                      },
                      {
                        "routing": {
                          "state": "INITIALIZING",
                          "primary": false,
                          "node": "boss",
                          "relocating_node": "underboss"
                        }
                      }
                    ],
                    "1": [
                      {
                        "routing": {
                          "state": "UNASSIGNED",
                          "primary": false,
                          "node": "underboss",
                          "relocating_node": null
                        }
                      }
                    ]
                  }
            }

            """;

    @BeforeEach
    void setUp() {
        toTest = new IndexStatisticsBuilder();
    }

    @Test
    void testIndexStatisticsBuildingWithOs3API() throws JsonProcessingException {
        final IndexStatistics built = toTest.build("graylog_42", IndicesStats.builder()
                .primaries(IndexStats.builder()
                        .flush(mockFlushStatsWithTotalAndTimeInMs(6, 1111))
                        .get(mockGetStatsWithTotalAndTimeInMs(7, 2222))
                        .indexing(mockIndexingStatsWithTotalAndTimeInMs(17, 30_000))
                        .merges(mockMergesStatsWithTotalAndTimeInMs(13, 3_600_000))
                        .refresh(mockRefreshStatsWithTotalAndTimeInMs(1, 2000))
                        .search(mockSearchStats(11, 3212, 22, 5212, 2))
                        .store(builder -> builder.sizeInBytes(1234567L).reservedInBytes(NVM))
                        .segments(mockSegmentsStats(12))
                        .docs(builder -> builder.count(111).deleted(42L))
                        .build())
                .total(IndexStats.builder()
                        .flush(mockFlushStatsWithTotalAndTimeInMs(6, 1111))
                        .get(mockGetStatsWithTotalAndTimeInMs(7, 2222))
                        .indexing(mockIndexingStatsWithTotalAndTimeInMs(17, 30_000))
                        .refresh(mockRefreshStatsWithTotalAndTimeInMs(1, 4000))
                        .search(mockSearchStats(11, 3111, 22, 5111, 2))
                        .store(builder -> builder.sizeInBytes(1234567L).reservedInBytes(NVM))
                        .segments(mockSegmentsStats(12))
                        .docs(builder -> builder.count(111).deleted(42L))
                        .build())
                .shards(
                        Map.of(
                                "0",
                                List.of(
                                        IndexShardStats.builder()
                                                .routing(ShardRouting.builder()
                                                        .state(ShardRoutingState.Started)
                                                        .primary(true)
                                                        .node("boss")
                                                        .relocatingNode("underboss")
                                                        .build()
                                                ).build(),
                                        IndexShardStats.builder()
                                                .routing(ShardRouting.builder()
                                                        .state(ShardRoutingState.Initializing)
                                                        .primary(false)
                                                        .node("boss")
                                                        .relocatingNode("underboss")
                                                        .build()
                                                ).build()
                                ),
                                "1",
                                List.of(
                                        IndexShardStats.builder()
                                                .routing(ShardRouting.builder()
                                                        .state(ShardRoutingState.Unassigned)
                                                        .primary(false)
                                                        .node("underboss")
                                                        .build()
                                                ).build()
                                )
                        )
                )
                .uuid("Whatever")
                .build());

        final IndexStatistics expected = IndexStatistics.create(
                "graylog_42",
                org.graylog2.rest.models.system.indexer.responses.IndexStats.create(
                        create(6, 1),
                        create(7, 2),
                        create(17, 30),
                        create(13, 60 * 60),
                        create(1, 2),
                        create(11, 3),
                        create(22, 5),
                        2L,
                        1234567L,
                        12,
                        org.graylog2.rest.models.system.indexer.responses.IndexStats.DocsStats.create(111, 42L)
                ),
                org.graylog2.rest.models.system.indexer.responses.IndexStats.create(
                        create(6, 1),
                        create(7, 2),
                        create(17, 30),
                        create(0, 0), //default values for missing stats
                        create(1, 4),
                        create(11, 3),
                        create(22, 5),
                        2L,
                        1234567L,
                        12,
                        org.graylog2.rest.models.system.indexer.responses.IndexStats.DocsStats.create(111, 42L)
                ),
                List.of(
                        org.graylog2.rest.models.system.indexer.responses.ShardRouting.create(0, "started", true, true, "boss", null, null, "underboss"),
                        org.graylog2.rest.models.system.indexer.responses.ShardRouting.create(0, "initializing", false, false, "boss", null, null, "underboss"),
                        org.graylog2.rest.models.system.indexer.responses.ShardRouting.create(1, "unassigned", false, false, "underboss", null, null, null)
                )
        );

        //verify builder returns what you expect it to return
        assertEquals(expected, built);

        //verify it does not differ from how it has been done so far
        final JsonNode jsonNode = objectMapperProvider.get().readTree(SAMPLE_STATS_RESPONSE);
        assertEquals(built, IndexStatistics.create("graylog_42", jsonNode));
    }

    private FlushStats mockFlushStatsWithTotalAndTimeInMs(final long total, final long timeInMillis) {
        FlushStats stats = Mockito.mock(FlushStats.class);
        doReturn(total).when(stats).total();
        doReturn(timeInMillis).when(stats).totalTimeInMillis();
        return stats;
    }

    private GetStats mockGetStatsWithTotalAndTimeInMs(final long total, final long timeInMillis) {
        GetStats stats = Mockito.mock(GetStats.class);
        doReturn(total).when(stats).total();
        doReturn(timeInMillis).when(stats).timeInMillis();
        return stats;
    }

    private IndexingStats mockIndexingStatsWithTotalAndTimeInMs(final long total, final long timeInMillis) {
        IndexingStats stats = Mockito.mock(IndexingStats.class);
        doReturn(total).when(stats).indexTotal();
        doReturn(timeInMillis).when(stats).indexTimeInMillis();
        return stats;
    }

    private MergesStats mockMergesStatsWithTotalAndTimeInMs(final long total, final long timeInMillis) {
        MergesStats stats = Mockito.mock(MergesStats.class);
        doReturn(total).when(stats).total();
        doReturn(timeInMillis).when(stats).totalTimeInMillis();
        return stats;
    }

    private RefreshStats mockRefreshStatsWithTotalAndTimeInMs(final long total, final long timeInMillis) {
        RefreshStats stats = Mockito.mock(RefreshStats.class);
        doReturn(total).when(stats).total();
        doReturn(timeInMillis).when(stats).totalTimeInMillis();
        return stats;
    }

    private SegmentsStats mockSegmentsStats(final int count) {
        SegmentsStats stats = Mockito.mock(SegmentsStats.class);
        doReturn(count).when(stats).count();
        return stats;
    }

    private SearchStats mockSearchStats(final long queryTotal,
                                        final long queryTimeInMillis,
                                        final long fetchTotal,
                                        final long fetchTimeInMillis,
                                        final long openContexts) {
        SearchStats stats = Mockito.mock(SearchStats.class);
        doReturn(queryTotal).when(stats).queryTotal();
        doReturn(queryTimeInMillis).when(stats).queryTimeInMillis();
        doReturn(fetchTotal).when(stats).fetchTotal();
        doReturn(fetchTimeInMillis).when(stats).fetchTimeInMillis();
        doReturn(openContexts).when(stats).openContexts();
        return stats;
    }

}
