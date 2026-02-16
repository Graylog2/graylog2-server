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

import org.bson.conversions.Bson;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.search.SearchQuery;

public class AbstractPaginatedNodeService<T extends NodeDto> {

    private final MongoPaginationHelper<T> paginationHelper;

    public AbstractPaginatedNodeService(MongoCollections mongoCollections, String collectionName, Class<T> nodeClass) {
        final MongoCollection<T> collection = mongoCollections.collection(collectionName, nodeClass);
        this.paginationHelper = mongoCollections.paginationHelper(collection);
    }

    public PaginatedList<T> searchPaginated(SearchQuery query,
                                            Bson sort, int page, int perPage) {
        return paginationHelper
                .filter(query.toBson())
                .sort(sort)
                .perPage(perPage)
                .page(page);
    }
}
