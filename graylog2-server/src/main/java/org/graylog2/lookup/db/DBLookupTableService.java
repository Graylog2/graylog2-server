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
package org.graylog2.lookup.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import jakarta.inject.Inject;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.entities.EntityScopeService;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.database.utils.ScopedEntityMongoUtils;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.lookup.dto.LookupTableDto;
import org.graylog2.lookup.events.LookupTablesDeleted;
import org.graylog2.lookup.events.LookupTablesUpdated;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static org.graylog2.database.utils.MongoUtils.stream;

public class DBLookupTableService {
    public static final String COLLECTION_NAME = "lut_tables";

    private final ClusterEventBus clusterEventBus;
    private final MongoCollection<LookupTableDto> collection;
    private final MongoUtils<LookupTableDto> mongoUtils;
    private final ScopedEntityMongoUtils<LookupTableDto> scopedEntityMongoUtils;
    private final MongoPaginationHelper<LookupTableDto> paginationHelper;

    @Inject
    public DBLookupTableService(MongoCollections mongoCollections,
                                EntityScopeService entityScopeService,
                                ClusterEventBus clusterEventBus) {
        this.clusterEventBus = clusterEventBus;
        this.collection = mongoCollections.collection(COLLECTION_NAME, LookupTableDto.class);
        this.mongoUtils = mongoCollections.utils(collection);
        this.scopedEntityMongoUtils = mongoCollections.scopedEntityUtils(collection, entityScopeService);
        this.paginationHelper = mongoCollections.paginationHelper(collection);
        collection.createIndex(Indexes.ascending("name"), new IndexOptions().unique(true));
    }

    public Optional<LookupTableDto> get(String idOrName) {
        if (ObjectId.isValid(idOrName)) {
            return mongoUtils.getById(idOrName);
        } else {
            // not an ObjectId, try with name
            return Optional.ofNullable(collection.find(eq("name", idOrName)).first());
        }
    }

    public LookupTableDto save(LookupTableDto table) {
        final LookupTableDto savedLookupTable;
        if (table.id() == null) {
            final String id = scopedEntityMongoUtils.create(table);
            savedLookupTable = table.toBuilder().id(id).build();
        } else {
            savedLookupTable = scopedEntityMongoUtils.update(table);
        }
        return savedLookupTable;
    }

    public LookupTableDto saveAndPostEvent(LookupTableDto table) {
        final LookupTableDto savedLookupTable = save(table);
        clusterEventBus.post(LookupTablesUpdated.create(savedLookupTable));

        return savedLookupTable;
    }

    public void postBulkUpdate(Collection<LookupTableDto> tables) {
        clusterEventBus.post(LookupTablesUpdated.create(tables));
    }

    public Collection<LookupTableDto> findAll() {
        return stream(collection.find()).toList();
    }

    public PaginatedList<LookupTableDto> findPaginated(Bson query, Bson sort, int page, int perPage) {
        return paginationHelper.filter(query).sort(sort).perPage(perPage).page(page);
    }

    public Collection<LookupTableDto> findByCacheIds(Collection<String> cacheIds) {
        Bson query = in("cache", cacheIds.stream().map(ObjectId::new).collect(Collectors.toList()));
        return stream(collection.find(query)).toList();
    }

    public Collection<LookupTableDto> findByDataAdapterIds(Collection<String> dataAdapterIds) {
        Bson query = in("data_adapter", dataAdapterIds.stream().map(ObjectId::new).collect(Collectors.toList()));
        return stream(collection.find(query)).toList();
    }

    public void deleteAndPostEvent(String idOrName) {
        final Optional<LookupTableDto> lookupTableDto = get(idOrName);
        lookupTableDto.ifPresent(lookupTable -> {
            scopedEntityMongoUtils.deleteById(lookupTable.id());
            clusterEventBus.post(LookupTablesDeleted.create(lookupTable));
        });
    }

    public void deleteAndPostEventImmutable(String idOrName) {
        final Optional<LookupTableDto> lookupTableDto = get(idOrName);
        lookupTableDto.ifPresent(lookupTable -> {
            scopedEntityMongoUtils.forceDelete(lookupTable.id());
            clusterEventBus.post(LookupTablesDeleted.create(lookupTable));
        });
    }

    public void forEach(Consumer<? super LookupTableDto> action) {
        stream(collection.find()).forEach(action);
    }
}
