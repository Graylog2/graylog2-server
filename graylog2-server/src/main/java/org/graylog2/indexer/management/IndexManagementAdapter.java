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
package org.graylog2.indexer.management;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Set;

/**
 * What Index Management reads from and does to the search backend, beyond {@link org.graylog2.indexer.indices.IndicesAdapter}:
 * every index (hidden and closed ones included), every shard copy with why it is unassigned, the nodes, and
 * shard allocation. Implemented per storage module; backends that can't support it throw
 * {@link UnsupportedOperationException}.
 */
public interface IndexManagementAdapter {
    /** Every index, hidden and closed ones included. */
    List<CatIndex> indices();

    /** Names of the indices on the warm tier (searchable snapshots). */
    Set<String> warmIndices();

    /** Clears the field data, query and request caches of one existing index. */
    void clearCache(String index);

    /** Every shard copy in the cluster, with why and since when it is unassigned. */
    List<CatShard> shards();

    /** Every node in the cluster. */
    List<CatNode> nodes();

    /**
     * The backend's own explanation of one shard copy's allocation, as returned by it. The response is deeply
     * nested and differs between versions, so it is returned as JSON for the caller to read what it needs.
     */
    JsonNode allocationExplain(String index, int shard, boolean primary);

    /**
     * Retries the allocation of shards that hit the allocation retry limit. Moves no data and forces no stale or
     * empty primaries.
     *
     * @return whether the backend acknowledged the request
     */
    boolean retryFailedAllocations();
}
