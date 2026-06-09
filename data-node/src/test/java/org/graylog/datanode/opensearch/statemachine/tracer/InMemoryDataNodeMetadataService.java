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

import org.graylog2.cluster.nodes.DataNodeMetadata;
import org.graylog2.cluster.nodes.DataNodeMetadataService;
import org.graylog2.cluster.nodes.OpensearchVersionsOverview;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class InMemoryDataNodeMetadataService implements DataNodeMetadataService {

    private final Map<String, DataNodeMetadata> store = new HashMap<>();

    @Override
    public void setOpensearchVersion(String nodeId, String version) {
        final DataNodeMetadata existing = store.get(nodeId);
        final String latestAvailable = existing != null ? existing.latestAvailableOpensearchVersion() : null;
        store.put(nodeId, new DataNodeMetadata(null, nodeId, version, latestAvailable));
    }

    @Override
    public void setLatestAvailableOpensearchVersion(String nodeId, String version) {
        final DataNodeMetadata existing = store.get(nodeId);
        final String opensearchVersion = existing != null ? existing.currentOpensearchVersion() : null;
        store.put(nodeId, new DataNodeMetadata(null, nodeId, opensearchVersion, version));
    }

    @Override
    public Optional<DataNodeMetadata> findByNodeId(String nodeId) {
        return Optional.ofNullable(store.get(nodeId));
    }

    @Override
    public OpensearchVersionsOverview getVersionsOverview() {
        return OpensearchVersionsOverview.of(new ArrayList<>(store.values()), Map.of());
    }
}
