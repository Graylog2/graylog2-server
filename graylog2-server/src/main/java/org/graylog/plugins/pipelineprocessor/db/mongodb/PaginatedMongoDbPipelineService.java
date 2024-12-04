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
package org.graylog.plugins.pipelineprocessor.db.mongodb;

import jakarta.inject.Inject;
import org.graylog.plugins.pipelineprocessor.db.PaginatedPipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.search.SearchQuery;

import java.util.function.Predicate;

public class PaginatedMongoDbPipelineService implements PaginatedPipelineService {
    private static final String COLLECTION_NAME = "pipeline_processor_pipelines";
    private final MongoPaginationHelper<PipelineDao> paginationHelper;

    @Inject
    public PaginatedMongoDbPipelineService(MongoCollections mongoCollections) {
        paginationHelper = mongoCollections.paginationHelper(COLLECTION_NAME, PipelineDao.class);
    }

    @Override
    public PaginatedList<PipelineDao> findPaginated(SearchQuery searchQuery, Predicate<PipelineDao> filter, int page, int perPage, String sortField, String order) {
        return paginationHelper
                .filter(searchQuery.toBson())
                .perPage(perPage)
                .sort(SortOrder.fromString(order).toBsonSort(sortField))
                .page(page);
    }
}
