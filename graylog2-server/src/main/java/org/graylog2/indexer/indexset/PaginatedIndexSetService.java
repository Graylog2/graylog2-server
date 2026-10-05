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
package org.graylog2.indexer.indexset;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.rest.models.SortOrder;

import java.util.function.Predicate;

import static org.graylog2.database.pagination.DefaultMongoPaginationHelper.DEFAULT_COLLATION_WITH_CASE_INSENSITIVE_SORTING;

/**
 * Paginated, sortable, and filterable access to index set configurations for the entity data table.
 * <p>
 * Filtering and sorting always run in MongoDB. Without a predicate, skip and limit run in MongoDB too.
 * With a predicate, the helper applies it after fetching and pages in memory, and the returned total
 * counts only documents that pass it, so page math stays correct for users with per-entity permissions.
 */
@Singleton
public class PaginatedIndexSetService {
    private final MongoPaginationHelper<IndexSetConfig> paginationHelper;

    @Inject
    public PaginatedIndexSetService(MongoCollections mongoCollections) {
        this.paginationHelper = mongoCollections
                .paginationHelper(MongoIndexSetService.COLLECTION_NAME, IndexSetConfig.class)
                .collation(DEFAULT_COLLATION_WITH_CASE_INSENSITIVE_SORTING);
    }

    /**
     * Pages entirely in MongoDB. Use this when the caller may read every index set.
     *
     * @param dbQuery   filter executed in MongoDB
     * @param page      1-based page number
     * @param perPage   page size
     * @param sortField document field to sort on
     * @param order     sort direction
     */
    public PaginatedList<IndexSetConfig> findPaginated(Bson dbQuery, int page, int perPage, String sortField, SortOrder order) {
        return helper(dbQuery, perPage, sortField, order).page(page);
    }

    /**
     * Pages with a post-fetch predicate, typically a per-entity permission check.
     *
     * @param dbQuery   filter executed in MongoDB
     * @param predicate filter applied in code after fetching
     * @param page      1-based page number
     * @param perPage   page size
     * @param sortField document field to sort on
     * @param order     sort direction
     * @return the requested page; {@code pagination().total()} counts only documents that pass the predicate
     */
    public PaginatedList<IndexSetConfig> findPaginated(Bson dbQuery,
                                                       Predicate<IndexSetConfig> predicate,
                                                       int page,
                                                       int perPage,
                                                       String sortField,
                                                       SortOrder order) {
        return helper(dbQuery, perPage, sortField, order).page(page, predicate);
    }

    private MongoPaginationHelper<IndexSetConfig> helper(Bson dbQuery, int perPage, String sortField, SortOrder order) {
        return paginationHelper
                .filter(dbQuery)
                .sort(order.toBsonSort(sortField))
                .perPage(perPage);
    }
}
