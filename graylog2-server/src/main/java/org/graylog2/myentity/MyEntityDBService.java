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
package org.graylog2.myentity;

import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.entities.PaginatedEntityDbService;
import org.mongojack.JacksonDBCollection;

import javax.inject.Inject;

/**
 * Specific MyEntity DB service implementation.
 */
public class MyEntityDBService extends PaginatedEntityDbService<MyEntity> {

    public static final String COLLECTION_NAME = "my_entities";
    private final JacksonDBCollection<MyEntity, ObjectId> dbCollection;

    @Inject
    public MyEntityDBService(MongoConnection mongoConnection, MongoJackObjectMapperProvider mapper) {
        super(mongoConnection, mapper, MyEntity.class, COLLECTION_NAME);

        dbCollection = JacksonDBCollection.wrap(
                mongoConnection.getDatabase().getCollection(COLLECTION_NAME),
                MyEntity.class,
                ObjectId.class,
                mapper.get());
    }

    // Inherit get, save, delete methods from parent PaginatedEntityDbService.
}
