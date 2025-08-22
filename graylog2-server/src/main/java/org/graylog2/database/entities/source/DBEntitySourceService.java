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

import jakarta.inject.Inject;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;

import static org.graylog2.database.utils.MongoUtils.objectIdEq;

public class DBEntitySourceService {

    public static final String COLLECTION_NAME = "entity_source";
    private final MongoCollection<EntitySource> collection;

    @Inject
    public DBEntitySourceService(MongoCollections mongoCollections) {
        this.collection = mongoCollections.collection(COLLECTION_NAME, EntitySource.class);
    }

    public void create(EntitySource entitySource) {
        collection.insertOne(entitySource);
    }

    public void deleteByEntityId(String entityId) {
        collection.deleteMany(objectIdEq(EntitySource.FIELD_ENTITY_ID, entityId));
    }
}
