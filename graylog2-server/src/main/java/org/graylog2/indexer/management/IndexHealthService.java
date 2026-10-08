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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.indexer.indices.IndicesAdapter;

import java.util.List;
import java.util.Set;

/**
 * Index Management's view of the search backend: every index, hidden and closed ones included, and the actions
 * Graylog's {@link org.graylog2.indexer.indices.Indices} service has no method for.
 */
@Singleton
public class IndexHealthService {
    private final IndexManagementAdapter indexManagementAdapter;
    private final IndicesAdapter indicesAdapter;

    @Inject
    public IndexHealthService(IndexManagementAdapter indexManagementAdapter, IndicesAdapter indicesAdapter) {
        this.indexManagementAdapter = indexManagementAdapter;
        this.indicesAdapter = indicesAdapter;
    }

    /** Every index, hidden and closed ones included. */
    public List<CatIndex> indices() {
        return indexManagementAdapter.indices();
    }

    /** Names of the indices on the warm tier. */
    public Set<String> warmIndices() {
        return indexManagementAdapter.warmIndices();
    }

    /** Clears the caches of one index; the name must be an exact, existing index. */
    public void clearCache(String index) {
        indexManagementAdapter.clearCache(index);
    }

    /**
     * Opens an index that isn't Graylog's. Graylog's own indices are reopened through {@code Indices#reopenIndex},
     * which marks them so retention skips them; that marker (a Graylog alias) doesn't belong on other indices.
     */
    public void openIndex(String index) {
        indicesAdapter.openIndex(index);
    }
}
