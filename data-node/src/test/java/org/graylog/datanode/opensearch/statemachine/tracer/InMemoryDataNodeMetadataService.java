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
package org.graylog.datanode.opensearch.statemachine.tracer;

import jakarta.annotation.Nullable;
import org.graylog2.cluster.nodes.DataNodeMetadata;
import org.graylog2.cluster.nodes.DataNodeMetadataService;
import org.graylog2.cluster.nodes.OpensearchVersionsOverview;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class InMemoryDataNodeMetadataService implements DataNodeMetadataService {

    private final Map<String, DataNodeMetadata> store = new HashMap<>();

    @Override
    public void setOpensearchVersions(String nodeId, String currentVersion, @Nullable String latestAvailableVersion) {
        final DataNodeMetadata existing = store.get(nodeId);
        final String resolvedLatestAvailable = latestAvailableVersion != null ? latestAvailableVersion
                : (existing != null ? existing.latestAvailableOpensearchVersion() : null);
        store.put(nodeId, new DataNodeMetadata(null, nodeId, currentVersion, resolvedLatestAvailable));
    }

    @Override
    public Optional<DataNodeMetadata> findByNodeId(String nodeId) {
        return Optional.ofNullable(store.get(nodeId));
    }

    @Override
    public OpensearchVersionsOverview getVersionsOverview() {
        return OpensearchVersionsOverview.of(new ArrayList<>(store.values()));
    }
}
