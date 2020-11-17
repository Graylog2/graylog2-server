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

import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.search.SearchQuery;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.http.util.TextUtils.isBlank;

public class PaginatedUserService extends PaginatedDbService<UserOverviewDTO> {
    private static final String COLLECTION_NAME = "users";

    @Inject
    public PaginatedUserService(MongoConnection mongoConnection,
                                MongoJackObjectMapperProvider mapper) {
        super(mongoConnection, mapper, UserOverviewDTO.class, COLLECTION_NAME);
    }

    public long count() {
        return db.count();
    }

    public PaginatedList<UserOverviewDTO> findPaginated(SearchQuery searchQuery, int page,
                                                        int perPage, String sortField, String order) {
        final DBQuery.Query dbQuery = searchQuery.toDBQuery();
        final DBSort.SortBuilder sortBuilder = getSortBuilder(order, sortField);
        return findPaginatedWithQueryAndSort(dbQuery, sortBuilder, page, perPage);
    }

    public PaginatedList<UserOverviewDTO> findPaginatedByUserId(SearchQuery searchQuery, int page,
                                                                int perPage, String sortField, String order,
                                                                Set<String> userIds) {
        final DBQuery.Query dbQuery = searchQuery.toDBQuery()
                .in("_id", userIds);
        final DBSort.SortBuilder sortBuilder = getSortBuilder(order, sortField);
        return findPaginatedWithQueryAndSort(dbQuery, sortBuilder, page, perPage);
    }

    public PaginatedList<UserOverviewDTO> findPaginatedByRole(SearchQuery searchQuery, int page,
                                                              int perPage, String sortField, String order,
                                                              Set<String> roleIds) {
        final DBQuery.Query dbQuery = searchQuery.toDBQuery()
                .in(UserImpl.ROLES, roleIds);
        final DBSort.SortBuilder sortBuilder = getSortBuilder(order, sortField);
        return findPaginatedWithQueryAndSort(dbQuery, sortBuilder, page, perPage);
    }

    public PaginatedList<UserOverviewDTO> findPaginatedByAuthServiceBackend(SearchQuery searchQuery,
                                                                            int page,
                                                                            int perPage,
                                                                            String sortField,
                                                                            String order,
                                                                            String authServiceBackendId) {
        checkArgument(!isBlank(authServiceBackendId), "authServiceBackendId cannot be blank");

        final DBQuery.Query query = DBQuery.and(
                DBQuery.is(UserImpl.AUTH_SERVICE_ID, Optional.of(authServiceBackendId)),
                searchQuery.toDBQuery()
        );
        return findPaginatedWithQueryAndSort(query, getSortBuilder(order, sortField), page, perPage);
    }
}
