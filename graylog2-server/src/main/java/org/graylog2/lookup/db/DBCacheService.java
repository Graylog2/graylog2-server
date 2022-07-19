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
import org.graylog2.lookup.dto.CacheDto;
import org.graylog2.lookup.events.CachesDeleted;
import org.graylog2.lookup.events.CachesUpdated;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DBCacheService extends ScopedEntityPaginatedDbService<CacheDto> {
    public static final String COLLECTION_NAME = "lut_caches";

    private final ClusterEventBus clusterEventBus;

    @Inject
    public DBCacheService(MongoConnection mongoConnection,
                          MongoJackObjectMapperProvider mapper,
                          EntityScopeService entityScopeService,
                          ClusterEventBus clusterEventBus) {
        super(mongoConnection, mapper, CacheDto.class, COLLECTION_NAME, entityScopeService);
        this.clusterEventBus = clusterEventBus;

        db.createIndex(new BasicDBObject("name", 1), new BasicDBObject("unique", true));
    }

    public Optional<CacheDto> get(String idOrName) {
        if (ObjectId.isValid(idOrName)) {
            return Optional.ofNullable(db.findOneById(new ObjectId(idOrName)));
        } else {
            // not an ObjectId, try with name
            return Optional.ofNullable(db.findOne(DBQuery.is("name", idOrName)));
        }
    }

    @Override
    public CacheDto save(CacheDto table) {
        final CacheDto savedCache = super.save(table);
        clusterEventBus.post(CachesUpdated.create(savedCache.id()));

        return savedCache;
    }

    public PaginatedList<CacheDto> findPaginated(DBQuery.Query query, DBSort.SortBuilder sort, int page, int perPage) {
        try (DBCursor<CacheDto> cursor = db.find(query)
                .sort(sort)
                .limit(perPage)
                .skip(perPage * Math.max(0, page - 1))) {

            return new PaginatedList<>(asImmutableList(cursor), cursor.count(), page, perPage);
        }
    }

    @Override
    public int delete(String idOrName) {
        final Optional<CacheDto> cacheDto = get(idOrName);
        int numDeleted = super.delete(idOrName);
        cacheDto.ifPresent(cache -> clusterEventBus.post(CachesDeleted.create(cache.id())));
        return numDeleted;
    }

    public Collection<CacheDto> findByIds(Set<String> idSet) {
        final DBQuery.Query query = DBQuery.in("_id", idSet.stream().map(ObjectId::new).collect(Collectors.toList()));
        try (DBCursor<CacheDto> cursor = db.find(query)) {
            return asImmutableList(cursor);
        }
    }

    public Collection<CacheDto> findAll() {
        try (DBCursor<CacheDto> cursor = db.find()) {
            return asImmutableList(cursor);
        }
    }
}
