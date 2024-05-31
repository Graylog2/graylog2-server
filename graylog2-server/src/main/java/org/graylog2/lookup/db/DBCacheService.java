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
import org.graylog2.lookup.dto.CacheDto;
import org.graylog2.lookup.events.CachesDeleted;
import org.graylog2.lookup.events.CachesUpdated;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static com.mongodb.client.model.Filters.eq;
import static org.graylog2.database.utils.MongoUtils.stream;
import static org.graylog2.database.utils.MongoUtils.stringIdsIn;


public class DBCacheService {
    public static final String COLLECTION_NAME = "lut_caches";

    private final ClusterEventBus clusterEventBus;
    private final MongoCollection<CacheDto> collection;
    private final MongoUtils<CacheDto> mongoUtils;
    private final ScopedEntityMongoUtils<CacheDto> scopedEntityMongoUtils;
    private final MongoPaginationHelper<CacheDto> paginationHelper;

    @Inject
    public DBCacheService(MongoCollections mongoCollections,
                          EntityScopeService entityScopeService,
                          ClusterEventBus clusterEventBus) {
        this.clusterEventBus = clusterEventBus;
        this.collection = mongoCollections.collection(COLLECTION_NAME, CacheDto.class);
        this.mongoUtils = mongoCollections.utils(collection);
        this.scopedEntityMongoUtils = mongoCollections.scopedEntityUtils(collection, entityScopeService);
        this.paginationHelper = mongoCollections.paginationHelper(collection);
        collection.createIndex(Indexes.ascending("name"), new IndexOptions().unique(true));
    }

    public Optional<CacheDto> get(String idOrName) {
        if (ObjectId.isValid(idOrName)) {
            return mongoUtils.getById(idOrName);
        } else {
            // not an ObjectId, try with name
            return Optional.ofNullable(collection.find(eq("name", idOrName)).first());
        }
    }

    public CacheDto save(CacheDto cache) {
        final CacheDto savedCache;
        if (cache.id() == null) {
            final String id = scopedEntityMongoUtils.create(cache);
            savedCache = cache.toBuilder().id(id).build();
        } else {
            savedCache = scopedEntityMongoUtils.update(cache);
        }
        return savedCache;
    }

    public CacheDto saveAndPostEvent(CacheDto cache) {
        final CacheDto savedCache = save(cache);
        clusterEventBus.post(CachesUpdated.create(savedCache.id()));

        return savedCache;
    }

    public void postBulkUpdate(Set<String> updatedCacheIds) {
        clusterEventBus.post(CachesUpdated.create(updatedCacheIds));
    }

    public PaginatedList<CacheDto> findPaginated(Bson query, Bson sort, int page, int perPage) {
        return paginationHelper.filter(query).sort(sort).perPage(perPage).page(page);
    }

    public void deleteAndPostEvent(String idOrName) {
        final Optional<CacheDto> cacheDto = get(idOrName);

        cacheDto.ifPresent(cache -> {
            scopedEntityMongoUtils.deleteById(cache.id());
            clusterEventBus.post(CachesDeleted.create(cache.id()));
        });
    }

    public void deleteAndPostEventImmutable(String idOrName) {
        final Optional<CacheDto> cacheDto = get(idOrName);
        cacheDto.ifPresent(cache -> {
            scopedEntityMongoUtils.forceDelete(cache.id());
            clusterEventBus.post(CachesDeleted.create(cache.id()));
        });
    }

    public Collection<CacheDto> findByIds(Set<String> idSet) {
        return stream(collection.find(stringIdsIn(idSet))).toList();
    }

    public Collection<CacheDto> findAll() {
        return stream(collection.find()).toList();
    }
}
