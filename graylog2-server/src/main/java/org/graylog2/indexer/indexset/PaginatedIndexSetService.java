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

import com.google.common.collect.ImmutableList;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.filtering.DbSortResolver;
import org.graylog2.database.pagination.MongoPaginationHelper;

import java.util.List;
import java.util.function.Predicate;

import static org.graylog2.database.pagination.DefaultMongoPaginationHelper.DEFAULT_COLLATION_WITH_CASE_INSENSITIVE_SORTING;

/**
 * Paginated, sortable, and filterable access to index set configurations for the entity data table.
 * <p>
 * Filtering and sorting run in MongoDB, including sorts that join another collection
 * ({@link IndexSetAttributeSorts}). Without a predicate, skip and limit run in MongoDB too. With a predicate,
 * the helper fetches every match, applies the predicate, and pages in memory. The total then counts only
 * documents that pass the predicate, so page math stays correct for users with per-entity permissions.
 * <p>
 * Filters must reference stored fields only. The predicate-free path counts with {@code countDocuments(filter)},
 * which cannot see fields that a sort's join adds.
 */
@Singleton
public class PaginatedIndexSetService {
    private final MongoPaginationHelper<IndexSetConfig> paginationHelper;
    private final MongoCollection<IndexSetConfig> indexSets;

    @Inject
    public PaginatedIndexSetService(MongoCollections mongoCollections) {
        this.indexSets = mongoCollections.collection(MongoIndexSetService.COLLECTION_NAME, IndexSetConfig.class);
        this.paginationHelper = mongoCollections.paginationHelper(indexSets)
                .collation(DEFAULT_COLLATION_WITH_CASE_INSENSITIVE_SORTING);
    }

    /**
     * Pages entirely in MongoDB. Use this when the caller may read every index set.
     *
     * @param dbQuery filter executed in MongoDB
     * @param sort    resolved sort, including any join stages the sort key needs
     * @param page    1-based page number
     * @param perPage page size; 0 means no limit
     */
    public PaginatedList<IndexSetConfig> findPaginated(Bson dbQuery, DbSortResolver.ResolvedSort sort, int page, int perPage) {
        return helper(dbQuery, sort, perPage).page(page);
    }

    /**
     * Pages with a post-fetch predicate, typically a per-entity permission check.
     *
     * @param dbQuery   filter executed in MongoDB
     * @param predicate filter applied in code after fetching
     * @param sort      resolved sort, including any join stages the sort key needs
     * @param page      1-based page number
     * @param perPage   page size; 0 means no limit
     * @return the requested page; {@code pagination().total()} counts only documents that pass the predicate
     */
    public PaginatedList<IndexSetConfig> findPaginated(Bson dbQuery,
                                                       Predicate<IndexSetConfig> predicate,
                                                       DbSortResolver.ResolvedSort sort,
                                                       int page,
                                                       int perPage) {
        return helper(dbQuery, sort, perPage).page(page, predicate);
    }

    /** Counts per category over the whole collection. Use this when the caller may read every index set. */
    public IndexSetCategoryCounts countCategories() {
        return new IndexSetCategoryCounts(
                indexSets.countDocuments(),
                indexSets.countDocuments(IndexSetCategory.USER.toBson()),
                indexSets.countDocuments(IndexSetCategory.SYSTEM.toBson()),
                indexSets.countDocuments(IndexSetCategory.ILLUMINATE.toBson()));
    }

    /** Counts per category over the index sets that pass the predicate, typically a per-entity permission check. */
    public IndexSetCategoryCounts countCategories(Predicate<IndexSetConfig> predicate) {
        return new IndexSetCategoryCounts(
                count(Filters.empty(), predicate),
                count(IndexSetCategory.USER.toBson(), predicate),
                count(IndexSetCategory.SYSTEM.toBson(), predicate),
                count(IndexSetCategory.ILLUMINATE.toBson(), predicate));
    }

    private long count(Bson filter, Predicate<IndexSetConfig> predicate) {
        return ImmutableList.copyOf(indexSets.find(filter)).stream().filter(predicate).count();
    }

    private MongoPaginationHelper<IndexSetConfig> helper(Bson dbQuery, DbSortResolver.ResolvedSort sort, int perPage) {
        return paginationHelper
                .filter(dbQuery)
                .sort(withDeterministicOrder(sort.sort()))
                .pipeline(sort.preSortStages())
                .postSortPipeline(sort.postSortStages())
                .perPage(perPage);
    }

    /**
     * Each page is its own query, and MongoDB returns documents with equal sort keys in no fixed order. Two
     * queries could then disagree on which of those documents fall on which page, so one shows up twice and
     * another never. Sorting equal keys by {@code _id} makes every page query agree. A sort that already names
     * {@code _id} is returned as is: merging the same key twice would overwrite the caller's direction.
     */
    private static Bson withDeterministicOrder(Bson sort) {
        final BsonDocument keys = sort.toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());
        return keys.containsKey("_id") ? sort : Sorts.orderBy(sort, Sorts.ascending("_id"));
    }
}
