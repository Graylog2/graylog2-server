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
package org.graylog2.cluster.nodes;

import jakarta.inject.Inject;
import org.graylog2.cluster.nodes.mongodb.MongodbNodesProvider;
import org.graylog2.database.PaginatedList;
import org.graylog2.search.SearchQuery;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class MongodbNodesServiceImpl implements MongodbNodesService {

    private final Set<MongodbNodesProvider> providers;

    @Inject
    public MongodbNodesServiceImpl(Set<MongodbNodesProvider> providers) {
        this.providers = providers;
    }

    @Override
    public PaginatedList<MongodbNode> searchPaginated(SearchQuery searchQuery, Comparator<MongodbNode> comparator, int page, int perPage) {

        final List<MongodbNode> allNodes = providers.stream()
                .filter(MongodbNodesProvider::available)
                .findFirst()
                .map(MongodbNodesProvider::allNodes)
                .orElseThrow(() -> new IllegalStateException("No available Mongodb nodes"));

        final int totalCount = allNodes.size();

        // Apply pagination
        final int offset = (page - 1) * perPage;
        final List<MongodbNode> paginatedNodes = allNodes.stream()
                .sorted(Comparator.nullsLast(comparator))
                .skip(offset)
                .limit(perPage)
                .toList();

        return new PaginatedList<>(paginatedNodes, totalCount, page, perPage);
    }
}
