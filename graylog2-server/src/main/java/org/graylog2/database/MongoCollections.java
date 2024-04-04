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
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.jackson.CustomJacksonCodecRegistry;

@Singleton
public class MongoCollections {

    private final ObjectMapper objectMapper;
    private final MongoConnection mongoConnection;

    @Inject
    public MongoCollections(MongoJackObjectMapperProvider objectMapperProvider, MongoConnection mongoConnection) {
        this.objectMapper = objectMapperProvider.get();
        this.mongoConnection = mongoConnection;
    }

    /**
     * Get a MongoCollection configured to use Jackson for serialization/deserialization of objects.
     */
    public <T> GraylogMongoCollection<T> get(String collectionName, Class<T> valueType) {
        final MongoCollection<T> collection = mongoConnection.getMongoDatabase().getCollection(collectionName, valueType);
        final CustomJacksonCodecRegistry jacksonCodecRegistry = new CustomJacksonCodecRegistry(this.objectMapper,
                collection.getCodecRegistry());
        jacksonCodecRegistry.addCodecForClass(valueType);
        return new DefaultGraylogMongoCollection<>(collection.withCodecRegistry(jacksonCodecRegistry));
    }
}
