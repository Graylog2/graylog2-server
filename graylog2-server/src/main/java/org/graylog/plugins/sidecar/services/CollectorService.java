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

import com.mongodb.client.MongoCollection;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.plugins.sidecar.rest.models.Collector;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.search.SearchQuery;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

@Singleton
public class CollectorService {
    public static final String COLLECTION_NAME = "sidecar_collectors";

    private final MongoCollection<Collector> collection;
    private final MongoUtils<Collector> mongoUtils;
    private final MongoPaginationHelper<Collector> paginationHelper;

    @Inject
    public CollectorService(MongoCollections mongoCollections) {
        collection = mongoCollections.collection(COLLECTION_NAME, Collector.class);
        mongoUtils = mongoCollections.utils(collection);
        paginationHelper = mongoCollections.paginationHelper(collection);
    }

    @Nullable
    public Collector find(String id) {
        return mongoUtils.getById(id).orElse(null);
    }

    @Nullable
    public Collector findByName(String name) {
        return collection.find(eq("name", name)).first();
    }

    @Nullable
    public Collector findByNameAndOs(String name, String operatingSystem) {
        return collection.find(
                and(
                        eq("name", name),
                        eq("node_operating_system", operatingSystem)
                )
        ).first();
    }

    public long count() {
        return collection.countDocuments();
    }

    public List<Collector> all() {
        return collection.find().into(new ArrayList<>());
    }

    public PaginatedList<Collector> findPaginated(SearchQuery searchQuery, int page, int perPage, String sortField,
                                                  SortOrder order) {
        return paginationHelper
                .filter(searchQuery.toBson())
                .sort(order.toBsonSort(sortField))
                .perPage(perPage)
                .page(page);
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
                request.defaultTemplate()
        );
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

    public Collector save(Collector collector) {
        return mongoUtils.save(collector);
    }

    public int delete(String id) {
        return mongoUtils.deleteById(id) ? 1 : 0;
    }

    public Optional<Collector> get(String id) {
        return mongoUtils.getById(id);
    }
}
