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
package org.graylog2.database.entities.source;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import jakarta.inject.Inject;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;

import java.util.Set;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.or;
import static com.mongodb.client.model.Updates.set;

public class DBEntitySourceService {

    public static final String COLLECTION_NAME = "entity_source";
    private final MongoCollection<EntitySource> collection;

    @Inject
    public DBEntitySourceService(MongoCollections mongoCollections) {
        this.collection = mongoCollections.collection(COLLECTION_NAME, EntitySource.class);

        collection.createIndex(Indexes.ascending(EntitySource.FIELD_ENTITY_ID), new IndexOptions().unique(true));
    }

    public void create(EntitySource entitySource) {
        collection.insertOne(entitySource);
    }

    public void updateParentId(String oldParentId, String newParentId) {
        final Bson filterByParentId = eq(EntitySource.FIELD_PARENT_ID, oldParentId);
        final Bson updateParentId = set(EntitySource.FIELD_PARENT_ID, newParentId);
        collection.updateMany(filterByParentId, updateParentId);
    }

    public void deleteByEntityId(String entityId) {
        bulkDeleteByEntityId(Set.of(entityId));
    }

    public void bulkDeleteByEntityId(Set<String> entityIds) {
        // Delete all EntitySource documents where entity_id or parent_id matches one of the given entity IDs
        final Set<ObjectId> entityObjectIds = entityIds.stream().map(ObjectId::new).collect(Collectors.toSet());
        final Bson filter = or(
                in(EntitySource.FIELD_ENTITY_ID, entityObjectIds),
                in(EntitySource.FIELD_PARENT_ID, entityObjectIds)
        );
        collection.deleteMany(filter);
    }
}
