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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class OutdatedIndexService {

    private final Indices indices;
    private final IndexSetRegistry indexSetRegistry;
    private final Cluster cluster;

    @Inject
    public OutdatedIndexService(Indices indices, IndexSetRegistry indexSetRegistry, Cluster cluster) {
        this.indices = indices;
        this.indexSetRegistry = indexSetRegistry;
        this.cluster = cluster;
    }

    public Set<OutdatedIndex> getOutdatedIndices() {
        int currentMajorVersion = Optional.ofNullable(cluster.elasticsearchStats().clusterVersion())
                .map(version -> {
                    try {
                        return Integer.parseInt(StringUtils.substringBefore(version, "."));
                    } catch (NumberFormatException e) {
                        throw new IllegalStateException("Cluster version cannot be determined: " + version);
                    }
                }).orElseThrow(() -> new IllegalStateException("Cluster version cannot be determined: null"));
        return indices.getOutdatedIndices(currentMajorVersion).stream()
                .map(index -> index.asManaged(indexSetRegistry.isManagedIndex(index.indexName())))
                .collect(Collectors.toSet());
    }

}
