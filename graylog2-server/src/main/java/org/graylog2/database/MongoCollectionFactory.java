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
import org.bson.UuidRepresentation;
import org.graylog.shaded.mongojack4.org.mongojack.JacksonMongoCollection;
import org.graylog.shaded.mongojack4.org.mongojack.ObjectMapperConfigurer;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MongoCollectionFactory {
    private final ObjectMapper objectMapper;
    private final MongoConnection mongoConnection;

    @Inject
    public MongoCollectionFactory(ObjectMapper objectMapper, MongoConnection mongoConnection) {
        this.objectMapper = ObjectMapperConfigurer.configureObjectMapper(objectMapper.copy());
        this.mongoConnection = mongoConnection;
    }

    public <T> GraylogMongoCollection<T> create(Class<T> valueType, String collectionName) {
        return new GraylogMongoCollectionImpl<>(createJacksonMongoCollection(valueType, collectionName));
    }

    private <T> JacksonMongoCollection<T> createJacksonMongoCollection(Class<T> valueType, String collectionName) {
        return JacksonMongoCollection.builder()
                .withObjectMapper(objectMapper)
                .build(mongoConnection.getMongoDatabase(), collectionName, valueType, UuidRepresentation.UNSPECIFIED);
    }
}
