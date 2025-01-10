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
package org.graylog2.tokenusage;

import com.mongodb.client.MongoCollection;
import jakarta.inject.Inject;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.tokenusage.TokenUsageDTO;
import org.graylog2.search.SearchQuery;
import org.graylog2.security.AccessTokenImpl;

public class PaginatedTokenUsageService {
    private static final String COLLECTION_NAME = AccessTokenImpl.COLLECTION_NAME;
    private final MongoPaginationHelper<TokenUsageDTO> paginationHelper;

    @Inject
    public PaginatedTokenUsageService(MongoCollections mongoCollections) {
        MongoCollection<TokenUsageDTO> collection = mongoCollections.collection(COLLECTION_NAME, TokenUsageDTO.class);
        paginationHelper = mongoCollections.paginationHelper(collection);
    }

    public PaginatedList<TokenUsageDTO> findPaginated(SearchQuery searchQuery, int page,
                                                      int perPage, String sortField, SortOrder order) {
        return paginationHelper
                .filter(searchQuery.toBson())
                .sort(order.toBsonSort(sortField))
                .perPage(perPage)
                .page(page);
    }
}
