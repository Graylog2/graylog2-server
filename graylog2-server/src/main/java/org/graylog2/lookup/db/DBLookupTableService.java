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

import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.entities.EntityScopeService;
import org.graylog2.database.entities.ScopedEntityPaginatedDbService;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.lookup.dto.LookupTableDto;
import org.graylog2.lookup.events.LookupTablesDeleted;
import org.graylog2.lookup.events.LookupTablesUpdated;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DBLookupTableService extends ScopedEntityPaginatedDbService<LookupTableDto> {
    public static final String COLLECTION_NAME = "lut_tables";

    private final ClusterEventBus clusterEventBus;

    @Inject
    public DBLookupTableService(MongoConnection mongoConnection,
                                MongoJackObjectMapperProvider mapper,
                                EntityScopeService entityScopeService,
                                ClusterEventBus clusterEventBus) {
        super(mongoConnection, mapper, LookupTableDto.class, COLLECTION_NAME, entityScopeService);
        this.clusterEventBus = clusterEventBus;

        db.createIndex(new BasicDBObject("name", 1), new BasicDBObject("unique", true));
    }

    @Override
    public Optional<LookupTableDto> get(String idOrName) {
        if (ObjectId.isValid(idOrName)) {
            return Optional.ofNullable(db.findOneById(new ObjectId(idOrName)));
        } else {
            // not an ObjectId, try with name
            return Optional.ofNullable(db.findOne(DBQuery.is("name", idOrName)));
        }
    }

    public LookupTableDto saveAndPostEvent(LookupTableDto table) {
        final LookupTableDto savedLookupTable = super.save(table);

        clusterEventBus.post(LookupTablesUpdated.create(savedLookupTable));

        return savedLookupTable;
    }

    public Collection<LookupTableDto> findAll() {
        return asImmutableList(db.find());
    }

    public PaginatedList<LookupTableDto> findPaginated(DBQuery.Query query, DBSort.SortBuilder sort, int page, int perPage) {
        try (DBCursor<LookupTableDto> cursor = db.find(query)
                .sort(sort)
                .limit(perPage)
                .skip(perPage * Math.max(0, page - 1))) {

            return new PaginatedList<>(asImmutableList(cursor), cursor.count(), page, perPage);
        }
    }

    public Collection<LookupTableDto> findByCacheIds(Collection<String> cacheIds) {
        final DBQuery.Query query = DBQuery.in("cache", cacheIds.stream().map(ObjectId::new).collect(Collectors.toList()));
        try (DBCursor<LookupTableDto> cursor = db.find(query)) {
            return asImmutableList(cursor);
        }
    }

    public Collection<LookupTableDto> findByDataAdapterIds(Collection<String> dataAdapterIds) {
        final DBQuery.Query query = DBQuery.in("data_adapter", dataAdapterIds.stream().map(ObjectId::new).collect(Collectors.toList()));
        try (DBCursor<LookupTableDto> cursor = db.find(query)) {
            return asImmutableList(cursor);
        }
    }

    public void deleteAndPostEvent(String idOrName) {
        final Optional<LookupTableDto> lookupTableDto = get(idOrName);
        super.delete(idOrName);
        lookupTableDto.ifPresent(lookupTable -> clusterEventBus.post(LookupTablesDeleted.create(lookupTable)));
    }

    public void deleteAndPostEventImmutable(String idOrName) {
        super.deleteImmutable(idOrName);
    }

    public void forEach(Consumer<? super LookupTableDto> action) {
        try (DBCursor<LookupTableDto> dbCursor = db.find()) {
            dbCursor.forEachRemaining(action);
        }
    }
}
