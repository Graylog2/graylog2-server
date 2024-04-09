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

import com.mongodb.client.MongoCollection;
import org.bson.types.ObjectId;

import java.util.Optional;

/**
 * A tiny wrapper for mongo collections that provides convenience methods beyond the official MongoDB client API.
 *
 * @param <T> Type of the documents in this collection.
 */
public interface GraylogMongoCollection<T> extends MongoCollection<T> {
    /**
     * A collection with a limited read-only API to return paginated lists of documents from MongoDB.
     */
    PaginatedCollection<T> findPaginated();

    Optional<T> getById(ObjectId id);

    default Optional<T> getById(String id) {
        return getById(new ObjectId(id));
    }

    boolean deleteById(ObjectId id);

    default boolean deleteById(String id) {
        return deleteById(new ObjectId(id));
    }

}
