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
package org.graylog2.users;

import com.mongodb.client.MongoCollection;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.search.SearchQuery;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static org.graylog2.database.utils.MongoUtils.stringIdsIn;

public class PaginatedUserService {
    private static final String COLLECTION_NAME = "users";
    private final MongoCollection<UserOverviewDTO> collection;
    private final MongoPaginationHelper<UserOverviewDTO> paginationHelper;

    @Inject
    public PaginatedUserService(MongoCollections mongoCollections) {
        collection = mongoCollections.collection(COLLECTION_NAME, UserOverviewDTO.class);
        paginationHelper = mongoCollections.paginationHelper(collection);
    }

    public long count() {
        return collection.countDocuments();
    }

    public PaginatedList<UserOverviewDTO> findPaginated(SearchQuery searchQuery, int page,
                                                        int perPage, String sortField, SortOrder order) {

        return paginationHelper
                .filter(searchQuery.toBson())
                .sort(order.toBsonSort(sortField))
                .perPage(perPage)
                .page(page);
    }

    public PaginatedList<UserOverviewDTO> findPaginatedByUserId(SearchQuery searchQuery, int page,
                                                                int perPage, String sortField, SortOrder order,
                                                                Set<String> userIds) {

        return paginationHelper
                .filter(and(searchQuery.toBson(), stringIdsIn(userIds)))
                .sort(order.toBsonSort(sortField))
                .perPage(perPage)
                .page(page);
    }

    public PaginatedList<UserOverviewDTO> findPaginatedByRole(SearchQuery searchQuery, int page,
                                                              int perPage, String sortField, SortOrder order,
                                                              Set<String> roleIds) {

        final var roleObjectIds = roleIds.stream().map(ObjectId::new).collect(Collectors.toSet());

        return paginationHelper
                .filter(and(searchQuery.toBson(), in(UserImpl.ROLES, roleObjectIds)))
                .sort(order.toBsonSort(sortField))
                .perPage(perPage)
                .page(page);
    }

    public PaginatedList<UserOverviewDTO> findPaginatedByAuthServiceBackend(SearchQuery searchQuery,
                                                                            int page,
                                                                            int perPage,
                                                                            String sortField,
                                                                            SortOrder order,
                                                                            String authServiceBackendId) {
        checkArgument(!StringUtils.isBlank(authServiceBackendId), "authServiceBackendId cannot be blank");

        return paginationHelper
                .filter(and(eq(UserImpl.AUTH_SERVICE_ID, authServiceBackendId), searchQuery.toBson()))
                .sort(order.toBsonSort(sortField))
                .perPage(perPage)
                .page(page);
    }

    public Stream<UserOverviewDTO> streamAll() {
        return MongoUtils.stream(collection.find());
    }
}
