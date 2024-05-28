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
package org.graylog.plugins.views.search.views;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import jakarta.inject.Inject;
import org.bson.conversions.Bson;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.SortOrder;

import java.util.List;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;

public class ViewSummaryService extends PaginatedDbService<ViewSummaryDTO> implements ViewUtils<ViewSummaryDTO> {
    private static final String COLLECTION_NAME = "views";
    private final MongoCollection<ViewSummaryDTO> collection;

    @Inject
    protected ViewSummaryService(MongoConnection mongoConnection,
                                 MongoJackObjectMapperProvider mongoJackObjectMapperProvider,
                                 MongoCollections mongoCollections) {
        super(mongoConnection, mongoJackObjectMapperProvider, ViewSummaryDTO.class, COLLECTION_NAME);
        this.collection = mongoCollections.collection(COLLECTION_NAME, ViewSummaryDTO.class);
    }

    public PaginatedList<ViewSummaryDTO> searchPaginatedByType(SearchUser searchUser,
                                                               ViewDTO.Type type,
                                                               Bson dbQuery, //query executed on DB level
                                                               Predicate<ViewSummaryDTO> predicate, //predicate executed on code level, AFTER data is fetched
                                                               SortOrder order,
                                                               String sortField,
                                                               int page,
                                                               int perPage) {
        checkNotNull(sortField);

        var sort = order.toBsonSort(sortField, ViewDTO.SECONDARY_SORT);

        var query = Filters.and(
                Filters.or(
                        Filters.eq(ViewDTO.FIELD_TYPE, type),
                        Filters.exists(ViewDTO.FIELD_TYPE, false)
                ),
                dbQuery
        );

        final List<ViewSummaryDTO> views = findViews(searchUser, query, sort)
                .filter(predicate)
                .toList();

        final List<ViewSummaryDTO> paginatedStreams = perPage > 0
                ? views.stream()
                .skip((long) perPage * Math.max(0, page - 1))
                .limit(perPage)
                .toList()
                : views;

        final long grandTotal = db.getCount(Filters.or(Filters.eq(ViewDTO.FIELD_TYPE, type), Filters.not(Filters.exists(ViewDTO.FIELD_TYPE))));

        return new PaginatedList<>(paginatedStreams, views.size(), page, perPage, grandTotal);
    }

    @Override
    public MongoCollection<ViewSummaryDTO> collection() {
        return collection;
    }
}
