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

package org.graylog2.indexer.indices;

import org.assertj.core.api.Assertions;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.system.stats.elasticsearch.ElasticsearchStats;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutdatedIndexServiceTest {

    @Mock
    Indices indices;

    @Mock
    Cluster cluster;

    @Mock
    IndexSetRegistry indexSetRegistry;

    @InjectMocks
    OutdatedIndexService outdatedIndexService;

    @Test
    void getOutdatedIndicesFailsIfNullOrIncorrectVersionProvided() {
        String errorMessage = "Cluster version cannot be determined: ";
        initializeElasticsearchStats(null);
        Assertions.assertThatThrownBy(() -> outdatedIndexService.getOutdatedIndices())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining(errorMessage + null);
        initializeElasticsearchStats("nodot");
        Assertions.assertThatThrownBy(() -> outdatedIndexService.getOutdatedIndices())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining(errorMessage + "nodot");
        initializeElasticsearchStats("not.sem.ver");
        Assertions.assertThatThrownBy(() -> outdatedIndexService.getOutdatedIndices())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining(errorMessage + "not.sem.ver");
    }

    @Test
    void getOutdatedIndicesSucceeds() {
        initializeElasticsearchStats("2.5.0");
        Set<OutdatedIndex> outdatedIndices = Set.of(
                new OutdatedIndex("outdated2", "1.3.0", true),
                new OutdatedIndex("outdated1", "1.3.0", false)
        );
        when(indexSetRegistry.isManagedIndex("outdated1")).thenReturn(true);
        when(indexSetRegistry.isManagedIndex("outdated2")).thenReturn(false);
        when(indices.getOutdatedIndices(2)).thenReturn(outdatedIndices);
        assertThat(outdatedIndexService.getOutdatedIndices()).isEqualTo(List.of(
                new OutdatedIndex("outdated1", "1.3.0", false, true),
                new OutdatedIndex("outdated2", "1.3.0", true, false)
        ));

    }

    private void initializeElasticsearchStats(String version) {
        ElasticsearchStats stats = mock(ElasticsearchStats.class);
        when(stats.clusterVersion()).thenReturn(version);
        when(cluster.elasticsearchStats()).thenReturn(stats);
    }

}
