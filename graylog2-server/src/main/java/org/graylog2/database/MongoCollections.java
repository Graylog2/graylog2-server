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
package org.graylog2.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import org.bson.UuidRepresentation;
import org.graylog.shaded.mongojack4.org.mongojack.JacksonMongoCollection;
import org.graylog.shaded.mongojack4.org.mongojack.internal.MongoJackModule;
import org.graylog2.bindings.providers.CommonMongoJackObjectMapperProvider;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class MongoCollections {

    private final ObjectMapper objectMapper;
    private final MongoConnection mongoConnection;

    @Inject
    public MongoCollections(CommonMongoJackObjectMapperProvider objectMapperProvider, MongoConnection mongoConnection) {
        this.objectMapper = objectMapperProvider.get();
        this.mongoConnection = mongoConnection;

        MongoJackModule.configure(this.objectMapper);
    }

    /**
     * Get a MongoCollection configured to use Mongojack for serialization/deserialization of objects.
     * <p>
     * <b>
     * To encourage usage of the mongodb driver API, we are intentionally not returning a {@link JacksonMongoCollection}
     * but rather the generic {@link MongoCollection} interface here.
     * </b>
     */
    public <T> MongoCollection<T> get(String collectionName, Class<T> valueType) {
        return JacksonMongoCollection.builder()
                .withObjectMapper(objectMapper)
                .build(mongoConnection.getMongoDatabase(), collectionName, valueType, UuidRepresentation.UNSPECIFIED);
    }
}
