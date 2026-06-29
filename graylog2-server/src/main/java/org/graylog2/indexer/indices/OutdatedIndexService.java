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
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.graylog2.shared.utilities.StringUtils.f;

@Singleton
public class OutdatedIndexService {

    private static final Logger LOG = LoggerFactory.getLogger(OutdatedIndexService.class);

    private final Indices indices;
    private final IndicesAdapter indicesAdapter;
    private final IndexSetRegistry indexSetRegistry;
    private final Cluster cluster;

    @Inject
    public OutdatedIndexService(Indices indices, IndicesAdapter indicesAdapter, IndexSetRegistry indexSetRegistry, Cluster cluster) {
        this.indices = indices;
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
        return indices.getOutdatedIndices(currentMajorVersion).stream()
                .map(index -> index.asManaged(indexSetRegistry.isManagedIndex(index.indexName())))
                .sorted().toList();
    }

    public void reindex(String index, boolean withReplicas) {
        HealthStatus sourceStatus = indices.waitForRecovery(index, 2);
        if (sourceStatus != HealthStatus.Green) {
            throw new IllegalStateException("Index " + index + " state is not healthy: " + sourceStatus);
        }
        // get settings & mapping of source index
        Map<String, Object> sourceSettings = indices.indexSettings(index);
        Map<String, Object> sourceMapping = indices.indexMapping(index);
        if (sourceSettings == null) {
            throw new IllegalStateException("No index sourceSettings found for index " + index);
        }
        // Remember how many documents the source holds so we can verify nothing is lost before each destructive step.
        final long sourceCount = indices.numberOfMessages(index);
        // clean index settings for creation
        Map<String, Object> tempSettings = cleanIndexSettings(sourceSettings, withReplicas);
        String tempIndex = ".gltmp_" + index.replaceAll("\\.", "");
        try {
            prepareTempIndex(index, tempIndex);

            // create and reindex into temp index
            indicesAdapter.create(tempIndex, new IndexSettings(tempSettings), sourceMapping);
            HealthStatus tempStatus = indices.waitForRecovery(tempIndex);
            if (tempStatus != HealthStatus.Green) {
                throw new IllegalStateException("Temporary index " + tempIndex + " could not be created successfully: " + tempStatus);
            }
            indices.reindex(index, tempIndex);
            indices.refresh(tempIndex);

            // Safety check: never delete the source index until the temporary index holds at least as many documents
            // as the source did. A shortfall means the copy lost documents, so we abort. We do not require exact
            // equality here — only a shortfall is evidence of lost source data, so we abort only when the temp index
            // has fewer.
            final long tempCount = indices.numberOfMessages(tempIndex);
            if (tempCount < sourceCount) {
                indicesAdapter.delete(tempIndex);
                throw new IllegalStateException(f("Aborting reindex of index %s: temporary index %s holds %d of %d " +
                        "documents. The source index was left untouched.", index, tempIndex, tempCount, sourceCount));
            }

            // ----- Point of no return: the source index is deleted next. -----
            // If anything fails from here on, the only complete copy of the data lives in the temporary index, so we
            // must never delete it in the failure path and we surface its name for manual recovery.
            indices.delete(index);
            try {
                // recreate and reindex into source index
                indicesAdapter.create(index, new IndexSettings(cleanIndexSettings(sourceSettings, true)), sourceMapping);
                // TODO: Benchmark if creating the target index with replicas 0 and reindexing and setting replicas afterwards is better
                //  (would need an additional health check before deleting temp)
                HealthStatus targetStatus = indices.waitForRecovery(index);
                if (targetStatus != HealthStatus.Green) {
                    throw new IllegalStateException("Index " + index + " could not be recreated successfully: " + targetStatus);
                }
                indices.reindex(tempIndex, index);
                indices.refresh(index);

                // The recreated index is built solely from the temp index, so its count must match tempCount exactly.
                // Verify against tempCount (not the original sourceCount) and abort on any mismatch.
                final long targetCount = indices.numberOfMessages(index);
                if (targetCount != tempCount) {
                    throw new IllegalStateException(f("Recreated index %s holds %d of %d documents.",
                            index, targetCount, tempCount));
                }
            } catch (Exception e) {
                throw new IllegalStateException(f("Reindexing index %s failed after the original index was deleted: %s. " +
                                "The complete data is preserved in temporary index %s and was NOT deleted; restore it manually.",
                        index, e.getMessage(), tempIndex), e);
            }

            // The recreated index is verified complete, so it is now safe to drop the temporary index.
            indices.delete(tempIndex);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Prepares the temporary index name for use. A leftover temporary index usually means a previous reindex run
     * failed. We only discard it when the source index still exists — if the source is gone, the temporary index may
     * hold the only surviving copy of the data and must not be deleted automatically.
     */
    private void prepareTempIndex(String index, String tempIndex) throws IOException {
        if (!indicesAdapter.exists(tempIndex)) {
            return;
        }
        if (indicesAdapter.exists(index)) {
            LOG.warn("Temporary index for reindexing already exists, deleting it: {}", tempIndex);
            indicesAdapter.delete(tempIndex);
        } else {
            throw new IllegalStateException(f("Refusing to reindex %s: the source index is missing but temporary index " +
                    "%s exists and may hold the only copy of the data from a previous failed reindex. Inspect and " +
                    "restore %s manually before retrying.", index, tempIndex, tempIndex));
        }
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
