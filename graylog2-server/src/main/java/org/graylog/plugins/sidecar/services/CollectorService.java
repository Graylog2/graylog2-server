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
package org.graylog.plugins.sidecar.services;

import org.graylog.plugins.sidecar.rest.models.Collector;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.search.SearchQuery;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class CollectorService extends PaginatedDbService<Collector> {
    public static final String COLLECTION_NAME = "sidecar_collectors";

    @Inject
    public CollectorService(MongoConnection mongoConnection,
                            MongoJackObjectMapperProvider mapper) {
        super(mongoConnection, mapper, Collector.class, COLLECTION_NAME);
    }

    @Nullable
    public Collector find(String id) {
        return db.findOne(DBQuery.is("_id", id));
    }

    @Nullable
    public Collector findByName(String name) {
        return db.findOne(DBQuery.is("name", name));
    }

    @Nullable
    public Collector findByNameAndOs(String name, String operatingSystem) {
        return db.findOne(
                DBQuery.and(
                        DBQuery.is("name", name),
                        DBQuery.is("node_operating_system", operatingSystem))
        );
    }

    @Nullable
    public Collector findByNameExcludeId(String name, String id) {
        return db.findOne(
                DBQuery.and(
                    DBQuery.is("name", name),
                    DBQuery.notEquals("_id", id))
        );
    }

    public long count() {
        return db.count();
    }

    public List<Collector> allFilter(Predicate<Collector> filter) {
        try (final Stream<Collector> collectorsStream = streamAll()) {
            final Stream<Collector> filteredStream = filter == null ? collectorsStream : collectorsStream.filter(filter);
            return filteredStream.collect(Collectors.toList());
        }
    }

    public List<Collector> all() {
        return allFilter(null);
    }

    public PaginatedList<Collector> findPaginated(SearchQuery searchQuery, int page, int perPage, String sortField, String order) {
        final DBQuery.Query dbQuery = searchQuery.toDBQuery();
        final DBSort.SortBuilder sortBuilder = getSortBuilder(order, sortField);
        return findPaginatedWithQueryAndSort(dbQuery, sortBuilder, page, perPage);
    }

    public Collector fromRequest(Collector request) {
        return Collector.create(
                null,
                request.name(),
                request.serviceType(),
                request.nodeOperatingSystem(),
                request.executablePath(),
                request.executeParameters(),
                request.validationParameters(),
                request.defaultTemplate());
    }

    public Collector fromRequest(String id, Collector request) {
        final Collector collector = fromRequest(request);
        return collector.toBuilder()
                .id(id)
                .build();
    }

    @Nullable
    public Collector copy(String id, String name) {
        Collector collector = find(id);
        return collector == null ? null : collector.toBuilder()
                .id(null)
                .name(name)
                .build();
    }
}
