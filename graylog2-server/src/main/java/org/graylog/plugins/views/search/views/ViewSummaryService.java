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

import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.search.SearchQuery;
import org.mongojack.DBQuery;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class ViewSummaryService extends PaginatedDbService<ViewSummaryDTO> {
    private static final String COLLECTION_NAME = "views";

    @Inject
    protected ViewSummaryService(MongoConnection mongoConnection,
                                 MongoJackObjectMapperProvider mapper) {
        super(mongoConnection, mapper, ViewSummaryDTO.class, COLLECTION_NAME);
    }

    private PaginatedList<ViewSummaryDTO> searchPaginated(DBQuery.Query query,
                                                   Predicate<ViewSummaryDTO> filter,
                                                   String order,
                                                   String sortField,
                                                   int page,
                                                   int perPage) {
        final PaginatedList<ViewSummaryDTO> viewsList = findPaginatedWithQueryFilterAndSort(query, filter, getSortBuilder(order, sortField), page, perPage);
        return viewsList.stream()
                .collect(Collectors.toCollection(() -> viewsList.grandTotal()
                        .map(grandTotal -> new PaginatedList<ViewSummaryDTO>(new ArrayList<>(viewsList.size()), viewsList.pagination().total(), page, perPage, grandTotal))
                        .orElseGet(() -> new PaginatedList<>(new ArrayList<>(viewsList.size()), viewsList.pagination().total(), page, perPage))));
    }

    public PaginatedList<ViewSummaryDTO> searchPaginatedByType(ViewDTO.Type type,
                                                        SearchQuery query,
                                                        Predicate<ViewSummaryDTO> filter,
                                                        String order,
                                                        String sortField,
                                                        int page,
                                                        int perPage) {
        checkNotNull(sortField);
        return searchPaginated(
                DBQuery.and(
                        DBQuery.or(DBQuery.is(ViewDTO.FIELD_TYPE, type), DBQuery.notExists(ViewDTO.FIELD_TYPE)),
                        query.toDBQuery()
                ),
                filter,
                order,
                sortField,
                page,
                perPage
        );
    }
}
