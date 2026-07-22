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
import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.system.stats.elasticsearch.ElasticsearchStats;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutdatedIndexServiceTest {

    @Mock
    IndicesAdapter indicesAdapter;

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
        IndexSet writeIndex = mock(IndexSet.class);
        when(writeIndex.getActiveWriteIndex()).thenReturn("outdated1");
        IndexSetConfig writeIndexConfig = mock(IndexSetConfig.class);
        when(writeIndex.getConfig()).thenReturn(writeIndexConfig);
        when(writeIndexConfig.id()).thenReturn("id1");
        when(indexSetRegistry.getForIndex("outdated1")).thenReturn(Optional.of(writeIndex));
        IndexSet noWriteIndex = mock(IndexSet.class);
        when(noWriteIndex.getActiveWriteIndex()).thenReturn("another_index");
        when(indexSetRegistry.getForIndex("outdated2")).thenReturn(Optional.of(noWriteIndex));
        when(indicesAdapter.getOutdatedIndices(2)).thenReturn(outdatedIndices);
        assertThat(outdatedIndexService.getOutdatedIndices()).isEqualTo(List.of(
                new OutdatedIndex("outdated1", "1.3.0", false, true, "id1"),
                new OutdatedIndex("outdated2", "1.3.0", true, false, null)
        ));

    }

    @Test
    void reindexFailsIfSourceIndexNotHealthy() {
        when(indicesAdapter.waitForRecovery("my_index", 2)).thenReturn(HealthStatus.Yellow);

        Assertions.assertThatThrownBy(() -> outdatedIndexService.reindex("my_index", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Index my_index state is not healthy: Yellow");
    }

    @Test
    void reindexFailsIfSourceSettingsAreNull() {
        when(indicesAdapter.waitForRecovery("my_index", 2)).thenReturn(HealthStatus.Green);
        when(indicesAdapter.getStructuredIndexSettings("my_index")).thenReturn(null);

        Assertions.assertThatThrownBy(() -> outdatedIndexService.reindex("my_index", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No index sourceSettings found for index my_index");
    }

    @Test
    void reindexFailsIfTempIndexIsNotHealthyAfterCreation() throws IOException {
        when(indicesAdapter.waitForRecovery("my_index", 2)).thenReturn(HealthStatus.Green);
        when(indicesAdapter.getStructuredIndexSettings("my_index")).thenReturn(sourceSettings());
        when(indicesAdapter.getIndexMapping("my_index")).thenReturn(sourceMapping());
        when(indicesAdapter.exists(".gltmp_my_index")).thenReturn(false);
        when(indicesAdapter.waitForRecovery(".gltmp_my_index")).thenReturn(HealthStatus.Red);

        Assertions.assertThatThrownBy(() -> outdatedIndexService.reindex("my_index", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Temporary index .gltmp_my_index could not be created successfully: Red");

        verify(indicesAdapter, never()).reindex(any(), any(), any());
        verify(indicesAdapter, never()).delete(any());
    }

    @Test
    void reindexFailsIfRecreatedTargetIndexIsNotHealthy() throws IOException {
        when(indicesAdapter.waitForRecovery("my_index", 2)).thenReturn(HealthStatus.Green);
        when(indicesAdapter.getStructuredIndexSettings("my_index")).thenReturn(sourceSettings());
        when(indicesAdapter.getIndexMapping("my_index")).thenReturn(sourceMapping());
        when(indicesAdapter.exists(".gltmp_my_index")).thenReturn(false);
        when(indicesAdapter.waitForRecovery(".gltmp_my_index")).thenReturn(HealthStatus.Green);
        when(indicesAdapter.waitForRecovery("my_index")).thenReturn(HealthStatus.Yellow);

        Assertions.assertThatThrownBy(() -> outdatedIndexService.reindex("my_index", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Index my_index could not be recreated successfully: Yellow");

        // The reindex into temp and the source delete have already happened, but the
        // final reindex back into the source must not run if the recreated index is unhealthy.
        verify(indicesAdapter).reindex(eq("my_index"), eq(".gltmp_my_index"), any());
        verify(indicesAdapter).delete("my_index");
        verify(indicesAdapter, never()).reindex(eq(".gltmp_my_index"), eq("my_index"), any());
        verify(indicesAdapter, never()).delete(".gltmp_my_index");
    }

    @Test
    void reindexWrapsIOExceptionInRuntimeException() throws IOException {
        when(indicesAdapter.waitForRecovery("my_index", 2)).thenReturn(HealthStatus.Green);
        when(indicesAdapter.getStructuredIndexSettings("my_index")).thenReturn(sourceSettings());
        when(indicesAdapter.getIndexMapping("my_index")).thenReturn(sourceMapping());
        when(indicesAdapter.exists(".gltmp_my_index")).thenThrow(new IOException("boom"));

        Assertions.assertThatThrownBy(() -> outdatedIndexService.reindex("my_index", true))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void reindexSucceedsAndPerformsAllStepsInOrder() throws IOException {
        Map<String, Object> sourceMapping = sourceMapping();
        when(indicesAdapter.waitForRecovery("my_index", 2)).thenReturn(HealthStatus.Green);
        when(indicesAdapter.getStructuredIndexSettings("my_index")).thenReturn(sourceSettings());
        when(indicesAdapter.getIndexMapping("my_index")).thenReturn(sourceMapping);
        when(indicesAdapter.exists(".gltmp_my_index")).thenReturn(false);
        when(indicesAdapter.waitForRecovery(".gltmp_my_index")).thenReturn(HealthStatus.Green);
        when(indicesAdapter.waitForRecovery("my_index")).thenReturn(HealthStatus.Green);

        outdatedIndexService.reindex("my_index", true);

        InOrder inOrder = inOrder(indicesAdapter, indicesAdapter);
        inOrder.verify(indicesAdapter).waitForRecovery("my_index", 2);
        inOrder.verify(indicesAdapter).getStructuredIndexSettings("my_index");
        inOrder.verify(indicesAdapter).getIndexMapping("my_index");
        inOrder.verify(indicesAdapter).exists(".gltmp_my_index");
        inOrder.verify(indicesAdapter).create(eq(".gltmp_my_index"), any(IndexSettings.class), eq(sourceMapping));
        inOrder.verify(indicesAdapter).waitForRecovery(".gltmp_my_index");
        inOrder.verify(indicesAdapter).reindex(eq("my_index"), eq(".gltmp_my_index"), any());
        inOrder.verify(indicesAdapter).delete("my_index");
        inOrder.verify(indicesAdapter).create(eq("my_index"), any(IndexSettings.class), eq(sourceMapping));
        inOrder.verify(indicesAdapter).waitForRecovery("my_index");
        inOrder.verify(indicesAdapter).reindex(eq(".gltmp_my_index"), eq("my_index"), any());
        inOrder.verify(indicesAdapter).delete(".gltmp_my_index");
    }

    @Test
    void reindexCleansSourceSettingsBeforeCreatingTempIndex() throws IOException {
        when(indicesAdapter.waitForRecovery("my_index", 2)).thenReturn(HealthStatus.Green);
        when(indicesAdapter.getStructuredIndexSettings("my_index")).thenReturn(sourceSettings());
        when(indicesAdapter.getIndexMapping("my_index")).thenReturn(sourceMapping());
        when(indicesAdapter.exists(".gltmp_my_index")).thenReturn(false);
        when(indicesAdapter.waitForRecovery(".gltmp_my_index")).thenReturn(HealthStatus.Green);
        when(indicesAdapter.waitForRecovery("my_index")).thenReturn(HealthStatus.Green);

        outdatedIndexService.reindex("my_index", true);

        ArgumentCaptor<IndexSettings> captor = ArgumentCaptor.forClass(IndexSettings.class);
        verify(indicesAdapter).create(eq(".gltmp_my_index"), captor.capture(), any());

        Map<String, Object> cleaned = captor.getValue().map();
        assertThat(cleaned).containsKey("index");
        assertThat(cleaned).containsEntry("top_level", "kept");
        @SuppressWarnings("unchecked")
        Map<String, Object> indexMap = (Map<String, Object>) cleaned.get("index");
        assertThat(indexMap).doesNotContainKeys("uuid", "version", "creation_date", "provided_name");
        assertThat(indexMap).containsEntry("number_of_shards", 4);
        assertThat(indexMap).containsEntry("number_of_replicas", 2);
    }

    @Test
    void reindexWithoutReplicasOverridesNumberOfReplicasOnTempIndex() throws IOException {
        when(indicesAdapter.waitForRecovery("my_index", 2)).thenReturn(HealthStatus.Green);
        when(indicesAdapter.getStructuredIndexSettings("my_index")).thenReturn(sourceSettings());
        when(indicesAdapter.getIndexMapping("my_index")).thenReturn(sourceMapping());
        when(indicesAdapter.exists(".gltmp_my_index")).thenReturn(false);
        when(indicesAdapter.waitForRecovery(".gltmp_my_index")).thenReturn(HealthStatus.Green);
        when(indicesAdapter.waitForRecovery("my_index")).thenReturn(HealthStatus.Green);

        outdatedIndexService.reindex("my_index", false);

        ArgumentCaptor<IndexSettings> tempCaptor = ArgumentCaptor.forClass(IndexSettings.class);
        verify(indicesAdapter).create(eq(".gltmp_my_index"), tempCaptor.capture(), any());
        @SuppressWarnings("unchecked")
        Map<String, Object> tempIndexMap = (Map<String, Object>) tempCaptor.getValue().map().get("index");
        assertThat(tempIndexMap).containsEntry("number_of_replicas", 0);
    }

    @Test
    void reindexStripsDotsFromIndexNameForTempIndex() throws IOException {
        when(indicesAdapter.waitForRecovery("graylog_2.0", 2)).thenReturn(HealthStatus.Green);
        when(indicesAdapter.getStructuredIndexSettings("graylog_2.0")).thenReturn(sourceSettings());
        when(indicesAdapter.getIndexMapping("graylog_2.0")).thenReturn(sourceMapping());
        when(indicesAdapter.exists(".gltmp_graylog_20")).thenReturn(false);
        when(indicesAdapter.waitForRecovery(".gltmp_graylog_20")).thenReturn(HealthStatus.Green);
        when(indicesAdapter.waitForRecovery("graylog_2.0")).thenReturn(HealthStatus.Green);

        outdatedIndexService.reindex("graylog_2.0", true);

        verify(indicesAdapter).exists(".gltmp_graylog_20");
        verify(indicesAdapter).create(eq(".gltmp_graylog_20"), any(IndexSettings.class), any());
        verify(indicesAdapter).reindex(eq("graylog_2.0"), eq(".gltmp_graylog_20"), any());
        verify(indicesAdapter).reindex(eq(".gltmp_graylog_20"), eq("graylog_2.0"), any());
    }

    @Test
    void reindexFailsIfSourceSettingsContainsNonMapValue() {
        Map<String, Object> badSettings = new HashMap<>();
        badSettings.put("index", "not_a_map");
        when(indicesAdapter.waitForRecovery("my_index", 2)).thenReturn(HealthStatus.Green);
        when(indicesAdapter.getStructuredIndexSettings("my_index")).thenReturn(badSettings);
        when(indicesAdapter.getIndexMapping("my_index")).thenReturn(sourceMapping());

        Assertions.assertThatThrownBy(() -> outdatedIndexService.reindex("my_index", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Settings key index is not a Map");
    }

    private void initializeElasticsearchStats(String version) {
        ElasticsearchStats stats = mock(ElasticsearchStats.class);
        when(stats.clusterVersion()).thenReturn(version);
        when(cluster.elasticsearchStats()).thenReturn(stats);
    }

    private Map<String, Object> sourceSettings() {
        Map<String, Object> indexMap = new HashMap<>();
        indexMap.put("number_of_shards", 4);
        indexMap.put("number_of_replicas", 2);
        indexMap.put("uuid", "abc-123");
        indexMap.put("version", Map.of("created", "7000099"));
        indexMap.put("creation_date", "1700000000000");
        indexMap.put("provided_name", "my_index");
        Map<String, Object> settings = new HashMap<>();
        settings.put("index", indexMap);
        settings.put("top_level", "kept");
        return settings;
    }

    private Map<String, Object> sourceMapping() {
        return Map.of("properties", Map.of("message", Map.of("type", "text")));
    }

}
