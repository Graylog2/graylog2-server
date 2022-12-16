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

import com.mongodb.BasicDBObject;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Variable;
import org.bson.Document;
import org.graylog.plugins.views.favorites.FavoritesService;
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
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkNotNull;

public class ViewSummaryService extends PaginatedDbService<ViewSummaryDTO> {
    private static final String COLLECTION_NAME = "views";
    private final MongoCollection<Document> collection;

    @Inject
    protected ViewSummaryService(MongoConnection mongoConnection,
                                 MongoJackObjectMapperProvider mapper) {
        super(mongoConnection, mapper, ViewSummaryDTO.class, COLLECTION_NAME);
        this.collection = mongoConnection.getMongoDatabase().getCollection(COLLECTION_NAME);
    }

    private PaginatedList<ViewSummaryDTO> searchPaginatedWithGrandTotal(SearchUser searchUser,
                                                                        ViewDTO.Type type,
                                                                        SearchQuery query,
                                                   Predicate<ViewSummaryDTO> filter,
                                                   String order,
                                                   String sortField,
                                                   DBQuery.Query grandTotalQuery,
                                                   int page,
                                                   int perPage) {
        return findPaginatedWithQueryFilterAndSortWithGrandTotal(searchUser, query, type, filter, getSortBuilder(order, sortField), grandTotalQuery, page, perPage);
    }

    protected PaginatedList<ViewSummaryDTO> findPaginatedWithQueryFilterAndSortWithGrandTotal(SearchUser searchUser,
                                                                                       SearchQuery dbQuery,
                                                                                       ViewDTO.Type type,
                                                                                       Predicate<ViewSummaryDTO> filter,
                                                                                       DBSort.SortBuilder sort,
                                                                                       DBQuery.Query grandTotalQuery,
                                                                                       int page,
                                                                                       int perPage) {


        var user = searchUser.getUser().getId();
        var query = Filters.and(
                // negation for Filters.exists() not found, so reverting to BasicDBObject for now
                Filters.or(Filters.eq(ViewDTO.FIELD_TYPE, type), new BasicDBObject(ViewDTO.FIELD_TYPE, new BasicDBObject("$exists", false))),
                dbQuery.toBson()
        );
        final AggregateIterable<Document> result = collection.aggregate(List.of(
                        Aggregates.match(query),
                        Aggregates.lookup(
                                FavoritesService.COLLECTION_NAME,
                                List.of(
                                        new Variable<>("searchId", doc("$toString", "$_id")),
                                        new Variable<>("userId", user)
                                ),
                                List.of(Aggregates.unwind("$items"),
                                        Aggregates.match(
                                                doc("$expr", doc("$and", List.of(
                                                                doc("$eq", List.of("$items.id", "$$searchId")),
                                                                doc("$eq", List.of("$user_id", "$$userId"))
                                                        )
                                                ))),
                                        Aggregates.project(doc("_id", 1))
                                ),
                                "favorites"
                        ),
                        Aggregates.set(new Field<>("favorite", doc("$gt", List.of(doc("$size", "$favorites"), 0)))),
                        Aggregates.unset("favorites"),
                        Aggregates.sort(sort)
                )
        );

        final long grandTotal = db.getCount(grandTotalQuery);

        final List<ViewSummaryDTO> views = StreamSupport.stream(result.spliterator(), false)
                .map(ViewSummaryDTO::fromDocument)
                .filter(filter)
                .toList();

        final List<ViewSummaryDTO> paginatedStreams = perPage > 0
                ? views.stream()
                .skip((long) perPage * Math.max(0, page - 1))
                .limit(perPage)
                .toList()
                : views;

        return new PaginatedList<>(paginatedStreams, views.size(), page, perPage, grandTotal);
    }

    private Document doc(String key, Object value) {
        return new Document(key, value);
    }

    public PaginatedList<ViewSummaryDTO> searchPaginatedByType(SearchUser searchUser,
                                                               ViewDTO.Type type,
                                                        SearchQuery query,
                                                        Predicate<ViewSummaryDTO> filter,
                                                        String order,
                                                        String sortField,
                                                        int page,
                                                        int perPage) {
        checkNotNull(sortField);
        return searchPaginatedWithGrandTotal(searchUser,
                type,
                query,
                filter,
                order,
                sortField,
                DBQuery.or(DBQuery.is(ViewDTO.FIELD_TYPE, type), DBQuery.notExists(ViewDTO.FIELD_TYPE)),
                page,
                perPage
        );
    }
}
