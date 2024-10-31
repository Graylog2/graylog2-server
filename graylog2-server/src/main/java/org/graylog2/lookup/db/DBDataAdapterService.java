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
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.lookup.events.DataAdaptersDeleted;
import org.graylog2.lookup.events.DataAdaptersUpdated;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static com.mongodb.client.model.Filters.eq;
import static org.graylog2.database.utils.MongoUtils.idEq;
import static org.graylog2.database.utils.MongoUtils.stream;
import static org.graylog2.database.utils.MongoUtils.stringIdsIn;

public class DBDataAdapterService {
    public static final String COLLECTION_NAME = "lut_data_adapters";

    private final ClusterEventBus clusterEventBus;
    private final MongoCollection<DataAdapterDto> collection;
    private final MongoUtils<DataAdapterDto> mongoUtils;
    private final ScopedEntityMongoUtils<DataAdapterDto> scopedEntityMongoUtils;
    private final MongoPaginationHelper<DataAdapterDto> paginationHelper;

    @Inject
    public DBDataAdapterService(MongoCollections mongoCollections,
                                EntityScopeService entityScopeService,
                                ClusterEventBus clusterEventBus) {
        this.clusterEventBus = clusterEventBus;
        this.collection = mongoCollections.collection(COLLECTION_NAME, DataAdapterDto.class);
        this.mongoUtils = mongoCollections.utils(collection);
        this.scopedEntityMongoUtils = mongoCollections.scopedEntityUtils(collection, entityScopeService);
        this.paginationHelper = mongoCollections.paginationHelper(collection);
        collection.createIndex(Indexes.ascending("name"), new IndexOptions().unique(true));
    }

    public Optional<DataAdapterDto> get(String idOrName) {
        if (ObjectId.isValid(idOrName)) {
            return mongoUtils.getById(idOrName);
        } else {
            // not an ObjectId, try with name
            return Optional.ofNullable(collection.find(eq("name", idOrName)).first());
        }
    }

    // Purposely bypass ScopedEntityMongoUtils checks and save the adapter. Only call this method internally and never
    // from user initiated API requests.
    public void forceSave(DataAdapterDto dataAdapter) {
        if (dataAdapter.id() == null) {
            collection.insertOne(dataAdapter);
        } else {
            collection.replaceOne(idEq(dataAdapter.id()), dataAdapter);
        }
    }

    public DataAdapterDto save(DataAdapterDto dataAdapter) {
        final DataAdapterDto savedDataAdapter;
        if (dataAdapter.id() == null) {
            final String id = scopedEntityMongoUtils.create(dataAdapter);
            savedDataAdapter = dataAdapter.toBuilder().id(id).build();
        } else {
            savedDataAdapter = scopedEntityMongoUtils.update(dataAdapter);
        }
        return savedDataAdapter;
    }

    public DataAdapterDto saveAndPostEvent(DataAdapterDto dataAdapter) {
        final DataAdapterDto savedDataAdapter = save(dataAdapter);
        clusterEventBus.post(DataAdaptersUpdated.create(savedDataAdapter.id()));

        return savedDataAdapter;
    }

    public void postBulkUpdate(Set<String> updatedAdapterIds) {
        clusterEventBus.post(DataAdaptersUpdated.create(updatedAdapterIds));
    }

    public PaginatedList<DataAdapterDto> findPaginated(Bson query, Bson sort, int page, int perPage) {
        return paginationHelper.filter(query).sort(sort).perPage(perPage).page(page);
    }

    public void deleteAndPostEvent(String idOrName) {
        final Optional<DataAdapterDto> dataAdapterDto = get(idOrName);
        dataAdapterDto.ifPresent(dataAdapter -> {
            scopedEntityMongoUtils.deleteById(dataAdapter.id());
            clusterEventBus.post(DataAdaptersDeleted.create(dataAdapter.id()));
        });
    }

    public void deleteAndPostEventImmutable(String idOrName) {
        final Optional<DataAdapterDto> dataAdapterDto = get(idOrName);
        dataAdapterDto.ifPresent(dataAdapter -> {
            scopedEntityMongoUtils.forceDelete(dataAdapter.id());
            clusterEventBus.post(DataAdaptersDeleted.create(dataAdapter.id()));
        });
    }

    public Collection<DataAdapterDto> findByIds(Set<String> idSet) {
        return stream(collection.find(stringIdsIn(idSet))).toList();

    }

    public Collection<DataAdapterDto> findAll() {
        return stream(collection.find()).toList();
    }
}
