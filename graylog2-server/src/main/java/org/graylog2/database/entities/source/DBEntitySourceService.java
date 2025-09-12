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
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.rest.resources.entities.FilterOption;

import java.util.Set;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;
import static org.graylog2.database.entities.source.EntitySource.USER_DEFINED;
import static org.graylog2.database.utils.MongoUtils.objectIdEq;

public class DBEntitySourceService {

    public static final String COLLECTION_NAME = "entity_source";
    private final MongoCollection<EntitySource> collection;
    public static final Set<FilterOption> FILTER_OPTIONS = Set.of(
            FilterOption.create(USER_DEFINED, "User Defined"),
            FilterOption.create("ILLUMINATE", "Illuminate"));

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
        collection.deleteMany(objectIdEq(EntitySource.FIELD_ENTITY_ID, entityId));
    }
}
