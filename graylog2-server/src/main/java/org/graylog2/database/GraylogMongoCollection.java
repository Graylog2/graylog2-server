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
     * Provides pagination for find operations.
     */
    PaginationProvider<T> findPaginated();

    /**
     * Convenience method to look up a single document by its ID.
     *
     * @param id the document's id.
     * @return the document wrapped in an {@link Optional} if present in the DB, an empty {@link Optional} otherwise.
     */
    Optional<T> getById(ObjectId id);

    /**
     * Convenience method to look up a single document by its ID.
     *
     * @param id HEX string representation of the document's {@link ObjectId}.
     * @return the document wrapped in an {@link Optional} if present in the DB, an empty {@link Optional} otherwise.
     */
    default Optional<T> getById(String id) {
        return getById(new ObjectId(id));
    }

    /**
     * Convenience method to delete a single document identified by its ID.
     *
     * @param id the document's id.
     * @return true if a document was deleted, false otherwise.
     */
    boolean deleteById(ObjectId id);

    /**
     * Convenience method to delete a single document identified by its ID.
     *
     * @param id HEX string representation of the document's {@link ObjectId}.
     * @return true if a document was deleted, false otherwise.
     */
    default boolean deleteById(String id) {
        return deleteById(new ObjectId(id));
    }

}
