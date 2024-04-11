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
package org.graylog2.database.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.types.ObjectId;
import org.graylog2.database.jackson.CustomJacksonCodecRegistry;
import org.mongojack.InitializationRequiredForTransformation;

import java.util.Optional;

public class DefaultMongoUtils<T> implements MongoUtils<T> {
    private final MongoCollection<T> collection;
    private final ObjectMapper objectMapper;
    private final CustomJacksonCodecRegistry codecRegistry;

    public DefaultMongoUtils(MongoCollection<T> delegate, ObjectMapper objectMapper) {
        this.collection = delegate;
        this.objectMapper = objectMapper;

        codecRegistry = new CustomJacksonCodecRegistry(
                objectMapper,
                collection.getCodecRegistry());
    }

    @Override
    public Optional<T> getById(ObjectId id) {
        return Optional.ofNullable(collection.find(Filters.eq("_id", id)).first());
    }

    @Override
    public Optional<T> getById(String id) {
        return getById(new ObjectId(id));
    }

    @Override
    public boolean deleteById(ObjectId id) {
        return collection.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
    }

    @Override
    public boolean deleteById(String id) {
        return deleteById(new ObjectId(id));
    }

    @Override
    public void initializeLegacyMongoJackBsonObject(InitializationRequiredForTransformation mongoJackBsonObject) {
        mongoJackBsonObject.initialize(
                objectMapper,
                objectMapper.constructType(collection.getDocumentClass()),
                codecRegistry);
    }

}
