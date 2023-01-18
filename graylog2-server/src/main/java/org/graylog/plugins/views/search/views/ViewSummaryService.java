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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.search.SearchQuery;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;

import javax.inject.Inject;
import java.util.List;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;

public class ViewSummaryService extends PaginatedDbService<ViewSummaryDTO> implements ViewUtils<ViewSummaryDTO> {
    private static final String COLLECTION_NAME = "views";
    private final MongoCollection<Document> collection;
    private final ObjectMapper mapper;

    @Inject
    protected ViewSummaryService(MongoConnection mongoConnection,
                                 MongoJackObjectMapperProvider mongoJackObjectMapperProvider,
                                 ObjectMapper mapper) {
        super(mongoConnection, mongoJackObjectMapperProvider, ViewSummaryDTO.class, COLLECTION_NAME);
        this.collection = mongoConnection.getMongoDatabase().getCollection(COLLECTION_NAME);
        this.mapper = mapper;
    }

    public PaginatedList<ViewSummaryDTO> searchPaginatedByType(SearchUser searchUser,
                                                                        ViewDTO.Type type,
                                                                        SearchQuery dbQuery,
                                                   Predicate<ViewSummaryDTO> filter,
                                                   String order,
                                                   String sortField,
                                                   int page,
                                                   int perPage) {

        checkNotNull(sortField);

        var sort = getSortBuilder(order, sortField);

        var query = Filters.and(
                // negation for Filters.exists() not found, so reverting to BasicDBObject for now
                Filters.or(Filters.eq(ViewDTO.FIELD_TYPE, type), new BasicDBObject(ViewDTO.FIELD_TYPE, new BasicDBObject("$exists", false))),
                dbQuery.toBson()
        );

        final List<ViewSummaryDTO> views = findViews(searchUser, query, sort)
                .map(this::deserialize)
                .filter(filter)
                .toList();

        final List<ViewSummaryDTO> paginatedStreams = perPage > 0
                ? views.stream()
                .skip((long) perPage * Math.max(0, page - 1))
                .limit(perPage)
                .toList()
                : views;

        final long grandTotal = db.getCount(DBQuery.or(DBQuery.is(ViewDTO.FIELD_TYPE, type), DBQuery.notExists(ViewDTO.FIELD_TYPE)));

        return new PaginatedList<>(paginatedStreams, views.size(), page, perPage, grandTotal);
    }

    @Override
    public ObjectMapper mapper() {
        return mapper;
    }

    @Override
    public MongoCollection<Document> collection() {
        return collection;
    }

    @Override
    public Class<ViewSummaryDTO> type() {
        return ViewSummaryDTO.class;
    }
}
