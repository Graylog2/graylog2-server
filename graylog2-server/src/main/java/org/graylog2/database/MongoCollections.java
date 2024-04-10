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
import org.graylog2.database.pagination.DefaultMongoPaginationHelper;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.database.utils.DefaultMongoUtils;
import org.graylog2.database.utils.MongoUtils;

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
     * Alias for {@link #getCollection(String, Class)}.
     *
     * @param collectionName Name of the collection
     * @param valueType      Java type of the documents stored in the collection
     * @return A collection using a Jackson codec for serialization and deserialization
     */
    public <T> MongoCollection<T> get(String collectionName, Class<T> valueType) {
        return getCollection(collectionName, valueType);
    }

    /**
     * Get a MongoCollection configured to use Jackson for serialization/deserialization of objects.
     *
     * @param collectionName Name of the collection
     * @param valueType      Java type of the documents stored in the collection
     * @return A collection using a Jackson codec for serialization and deserialization
     */
    public <T> MongoCollection<T> getCollection(String collectionName, Class<T> valueType) {
        final MongoCollection<T> collection = mongoConnection.getMongoDatabase().getCollection(collectionName, valueType);
        final CustomJacksonCodecRegistry jacksonCodecRegistry = new CustomJacksonCodecRegistry(this.objectMapper,
                collection.getCodecRegistry());
        jacksonCodecRegistry.addCodecForClass(valueType);
        return collection.withCodecRegistry(jacksonCodecRegistry);
    }

    /**
     * Provides a helper to perform find operations on a collection that yield pages of documents.
     */
    public <T> MongoPaginationHelper<T> getPaginationHelper(String collectionName, Class<T> valueType) {
        return getPaginationHelper(getCollection(collectionName, valueType));
    }

    /**
     * Provides a helper to perform find operations on a collection that yield pages of documents.
     */
    public <T> MongoPaginationHelper<T> getPaginationHelper(MongoCollection<T> collection) {
        return new DefaultMongoPaginationHelper<>(collection);
    }

    public <T> MongoUtils<T> getUtils(MongoCollection<T> collection) {
        return new DefaultMongoUtils<>(collection);
    }

    // TODO
//    public <T> ScopedMongoCollection getScopedCollection(String collectionName, Class<T> valueType) {
//        return new ScopedMongoCollection<>(getCollection(collectionName, valueType));
//    }
}
