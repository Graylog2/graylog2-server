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
package org.graylog.storage.opensearch2;

import com.fasterxml.jackson.databind.JsonNode;
import org.graylog2.indexer.management.CatIndex;
import org.graylog2.indexer.management.CatNode;
import org.graylog2.indexer.management.CatShard;
import org.graylog2.indexer.management.IndexManagementAdapter;

import java.util.List;
import java.util.Set;

public class IndexManagementAdapterOS2 implements IndexManagementAdapter {
    private static final String ERROR_MESSAGE = "Index Management needs Graylog's OpenSearch 3 client (feature flag opensearch3_client)";

    @Override
    public List<CatIndex> indices() {
        throw new UnsupportedOperationException(ERROR_MESSAGE);
    }

    @Override
    public Set<String> warmIndices() {
        throw new UnsupportedOperationException(ERROR_MESSAGE);
    }

    @Override
    public void clearCache(String index) {
        throw new UnsupportedOperationException(ERROR_MESSAGE);
    }

    @Override
    public List<CatShard> shards() {
        throw new UnsupportedOperationException(ERROR_MESSAGE);
    }

    @Override
    public List<CatNode> nodes() {
        throw new UnsupportedOperationException(ERROR_MESSAGE);
    }

    @Override
    public JsonNode allocationExplain(String index, int shard, boolean primary) {
        throw new UnsupportedOperationException(ERROR_MESSAGE);
    }

    @Override
    public boolean retryFailedAllocations() {
        throw new UnsupportedOperationException(ERROR_MESSAGE);
    }
}
