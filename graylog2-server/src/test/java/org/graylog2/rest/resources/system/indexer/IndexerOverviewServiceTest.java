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
package org.graylog2.rest.resources.system.indexer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.indexer.counts.Counts;
import org.graylog2.indexer.indexset.basic.BasicIndexSet;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.rest.models.system.deflector.responses.DeflectorSummary;
import org.graylog2.rest.models.system.indexer.responses.ClusterHealth;
import org.graylog2.rest.models.system.indexer.responses.ClusterName;
import org.graylog2.rest.models.system.indexer.responses.IndexSizeSummary;
import org.graylog2.rest.models.system.indexer.responses.IndexerOverview;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexerOverviewServiceTest {

    @Mock
    IndexerClusterResource indexerClusterResource;

    @Mock
    Counts counts;

    @Mock
    Indices indices;

    @Mock
    IndicesAdapter indicesAdapter;

    @Mock
    BasicIndexSet indexSet;

    @InjectMocks
    IndexerOverviewService indexerOverviewService;

    @Test
    void reportsPrimaryAndTotalSizeOfIndex() throws Exception {
        final JsonNode indexStats = new ObjectMapper().readTree("""
                {
                  "graylog_0": {
                    "primaries": {"docs": {"count": 100, "deleted": 2}, "store": {"size_in_bytes": 1024}},
                    "total": {"docs": {"count": 200, "deleted": 4}, "store": {"size_in_bytes": 3072}}
                  }
                }
                """);
        when(indices.getIndexStats(indexSet)).thenReturn(indexStats);
        when(indices.areReopened(List.of("graylog_0"))).thenReturn(Map.of("graylog_0", false));
        when(indexerClusterResource.clusterHealth()).thenReturn(mock(ClusterHealth.class));
        when(indexerClusterResource.clusterName()).thenReturn(ClusterName.create("graylog"));

        final IndexerOverview overview = indexerOverviewService.getIndexerOverview(
                indexSet, DeflectorSummary.create(true, "graylog_0"), List.of());

        assertThat(overview.indices())
                .singleElement()
                .satisfies(summary -> assertThat(summary.size()).isEqualTo(IndexSizeSummary.create(100, 2, 1024, 3072)));
    }
}
