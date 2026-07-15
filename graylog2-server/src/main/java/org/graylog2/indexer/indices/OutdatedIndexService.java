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

import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.NotNull;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.indexer.security.IndexerAdminCert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Singleton
public class OutdatedIndexService {

    private static final Logger LOG = LoggerFactory.getLogger(OutdatedIndexService.class);

    private final IndicesAdapter indicesAdapter;
    private final IndexSetRegistry indexSetRegistry;
    private final Cluster cluster;

    @Inject
    public OutdatedIndexService(@IndexerAdminCert IndicesAdapter indicesAdapter, IndexSetRegistry indexSetRegistry, Cluster cluster) {
        this.indicesAdapter = indicesAdapter;
        this.indexSetRegistry = indexSetRegistry;
        this.cluster = cluster;
    }

    public List<OutdatedIndex> getOutdatedIndices() {
        int currentMajorVersion = Optional.ofNullable(cluster.elasticsearchStats().clusterVersion())
                .map(version -> {
                    try {
                        return (int) Version.parse(version).majorVersion();
                    } catch (ParseException e) {
                        throw new IllegalStateException("Cluster version cannot be determined: " + version);
                    }
                }).orElseThrow(() -> new IllegalStateException("Cluster version cannot be determined: null"));
        return indicesAdapter.getOutdatedIndices(currentMajorVersion).stream()
                .map(index -> index.asManaged(indexSetRegistry.isManagedIndex(index.indexName())))
                .map(index -> index.asActiveWriteIndex(isActiveWriteIndexForSet(index.indexName())))
                .sorted().toList();
    }

    public String isActiveWriteIndexForSet(String index) {
        return indexSetRegistry.getForIndex(index)
                .filter(indexSet -> Objects.equals(indexSet.getActiveWriteIndex(), index))
                .map(indexSet -> indexSet.getConfig().id())
                .orElse(null);
    }

    public void reindex(String index, boolean withReplicas) {
        HealthStatus sourceStatus = indicesAdapter.waitForRecovery(index, 2);
        if (sourceStatus != HealthStatus.Green) {
            throw new IllegalStateException("Index " + index + " state is not healthy: " + sourceStatus);
        }
        // get settings & mapping of source index
        Map<String, Object> sourceSettings = indicesAdapter.getStructuredIndexSettings(index);
        Map<String, Object> sourceMapping = indicesAdapter.getIndexMapping(index);
        if (sourceSettings == null) {
            throw new IllegalStateException("No index sourceSettings found for index " + index);
        }
        // clean index settings for creation
        Map<String, Object> tempSettings = cleanIndexSettings(sourceSettings, withReplicas);
        String tempIndex = ".gltmp_" + index.replaceAll("\\.", "");
        try {
            // create and reindex into temp index
            if (indicesAdapter.exists(tempIndex)) {
                LOG.warn("Temporary index for reindexing already exists, deleting it: {}", tempIndex);
                indicesAdapter.delete(tempIndex);
            }
            indicesAdapter.create(tempIndex, new IndexSettings(tempSettings), sourceMapping);
            HealthStatus tempStatus = indicesAdapter.waitForRecovery(tempIndex);
            if (tempStatus != HealthStatus.Green) {
                throw new IllegalStateException("Temporary index " + tempIndex + " could not be created successfully: " + tempStatus);
            }
            reindex(index, tempIndex);
            // delete source index
            indicesAdapter.refresh(tempIndex);
            indicesAdapter.delete(index);
            // recreate and reindex into source index
            indicesAdapter.create(index, new IndexSettings(cleanIndexSettings(sourceSettings, true)), sourceMapping);
            // TODO: Benchmark if creating the target index with replicas 0 and reindexing and setting replicas afterwards is better
            //  (would need an additional health check before deleting temp)
            HealthStatus targetStatus = indicesAdapter.waitForRecovery(index);
            if (targetStatus != HealthStatus.Green) {
                throw new IllegalStateException("Index " + index + " could not be recreated successfully: " + targetStatus);
            }
            reindex(tempIndex, index);
            indicesAdapter.refresh(index);
            // delete temp index
            indicesAdapter.delete(tempIndex);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void reindex(String source, String target) {
        indicesAdapter.reindex(source, target, result -> {
            LOG.info("Reindexing index <{}> to <{}>: Bulk indexed {} messages, took {} ms, failures: {}",
                    source,
                    target,
                    result,
                    result.tookMs(),
                    result.hasFailedItems());

            if (result.hasFailedItems()) {
                throw new ElasticsearchException("Failed to reindex a message. Check your indexer log.");
            }
        });
    }

    private Map<String, Object> cleanIndexSettings(Map<String, Object> settings, boolean withReplicas) {
        // Keys to remove from the nested 'index' map
        final List<String> keysToRemove = List.of("uuid", "version", "creation_date", "provided_name");
        HashMap<String, Object> cleaned = new HashMap<>();
        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            if (!"index".equals(entry.getKey())) {
                cleaned.put(entry.getKey(), entry.getValue());
            } else if (entry.getValue() instanceof Map) {
                //noinspection unchecked
                Map<String, Object> indexMap = new HashMap<>((Map<String, Object>) entry.getValue());
                keysToRemove.forEach(indexMap.keySet()::remove);
                if (!withReplicas) {
                    indexMap.put("number_of_replicas", 0);
                }
                cleaned.put("index", indexMap);
            } else {
                throw new IllegalStateException("Settings key " + entry.getKey() + " is not a Map");
            }
        }
        return cleaned;
    }

    public void delete(@NotNull String index) {
        indicesAdapter.delete(index);
    }
}
