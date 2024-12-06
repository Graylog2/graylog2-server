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
import org.graylog.plugins.pipelineprocessor.db.PaginatedRuleService;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.search.SearchQuery;

public class PaginatedMongoDbRuleService implements PaginatedRuleService {
    private static final String COLLECTION_NAME = "pipeline_processor_rules";
    private final MongoPaginationHelper<RuleDao> paginationHelper;

    @Inject
    public PaginatedMongoDbRuleService(MongoCollections mongoCollections) {
        paginationHelper = mongoCollections.paginationHelper(COLLECTION_NAME, RuleDao.class);
    }

    @Override
    public PaginatedList<RuleDao> findPaginated(SearchQuery searchQuery, int page, int perPage, String sortField, String order) {
        return paginationHelper
                .filter(searchQuery.toBson())
                .perPage(perPage)
                .sort(SortOrder.fromString(order).toBsonSort(sortField))
                .page(page);
    }
}
