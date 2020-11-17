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

import com.google.common.collect.ImmutableList;
import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedList;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.lookup.dto.LookupTableDto;
import org.graylog2.lookup.events.LookupTablesDeleted;
import org.graylog2.lookup.events.LookupTablesUpdated;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DBLookupTableService {
    private final JacksonDBCollection<LookupTableDto, ObjectId> db;
    private final ClusterEventBus clusterEventBus;

    @Inject
    public DBLookupTableService(MongoConnection mongoConnection,
                                MongoJackObjectMapperProvider mapper,
                                ClusterEventBus clusterEventBus) {
        this.db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection("lut_tables"),
                LookupTableDto.class,
                ObjectId.class,
                mapper.get());
        this.clusterEventBus = clusterEventBus;

        db.createIndex(new BasicDBObject("name", 1), new BasicDBObject("unique", true));
    }

    public Optional<LookupTableDto> get(String idOrName) {
        try {
            return Optional.ofNullable(db.findOneById(new ObjectId(idOrName)));
        } catch (IllegalArgumentException e) {
            // not an ObjectId, try again with name
            return Optional.ofNullable(db.findOne(DBQuery.is("name", idOrName)));

        }
    }

    public LookupTableDto save(LookupTableDto table) {
        WriteResult<LookupTableDto, ObjectId> save = db.save(table);
        final LookupTableDto savedLookupTable = save.getSavedObject();

        clusterEventBus.post(LookupTablesUpdated.create(savedLookupTable));

        return savedLookupTable;
    }

    public Collection<LookupTableDto> findAll() {
        return asImmutableList(db.find());
    }

    public Collection<LookupTableDto> findByNames(Collection<String> names) {
        final DBQuery.Query query = DBQuery.in("name", names);
        final DBCursor<LookupTableDto> dbCursor = db.find(query);
        return asImmutableList(dbCursor);
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

    private ImmutableList<LookupTableDto> asImmutableList(Iterator<? extends LookupTableDto> cursor) {
        return ImmutableList.copyOf(cursor);
    }

    public void delete(String idOrName) {
        final Optional<LookupTableDto> lookupTableDto = get(idOrName);
        lookupTableDto
                .map(LookupTableDto::id)
                .map(ObjectId::new)
                .ifPresent(db::removeById);
        lookupTableDto.ifPresent(lookupTable -> clusterEventBus.post(LookupTablesDeleted.create(lookupTable)));
    }

    public void forEach(Consumer<? super LookupTableDto> action) {
        try (DBCursor<LookupTableDto> dbCursor = db.find()) {
            dbCursor.forEachRemaining(action);
        }
    }
}
