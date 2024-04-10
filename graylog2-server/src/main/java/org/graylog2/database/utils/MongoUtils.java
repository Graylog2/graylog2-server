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

import com.google.common.collect.Streams;
import com.mongodb.client.FindIterable;
import com.mongodb.client.result.InsertOneResult;
import jakarta.annotation.Nullable;
import org.bson.BsonObjectId;
import org.bson.BsonValue;
import org.bson.types.ObjectId;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Utility methods to interact with MongoDB collections.
 * <p>
 * Most of the utility methods only work correctly when the collection meets the following criteria, which are
 * considered best practices:
 * <ul>
 *     <li>The documents in the collection have an "_id" field and the value of this field is of type ObjectId</li>
 * </ul>
 *
 * @param <T> Java type of the documents to interact with
 */
public interface MongoUtils<T> {

    /**
     * Extract the inserted id as an {@link ObjectId} from the insert result.
     *
     * @param result Result of the insertOne operation
     * @return the inserted object ID, or null if no id was inserted. Fails if the id is not of type {@link ObjectId}.
     */
    @Nullable
    static ObjectId insertedId(@Nonnull InsertOneResult result) {
        return Optional.ofNullable(result.getInsertedId())
                .map(BsonValue::asObjectId)
                .map(BsonObjectId::getValue)
                .orElse(null);
    }

    /**
     * Extract the inserted id as a String from the insert result.
     *
     * @param result Result of the insertOne operation
     * @return the inserted object ID as string, or null if no id was inserted. Fails if the id is not of type {@link ObjectId}.
     */
    @Nullable
    static String insertedIdAsString(@Nonnull InsertOneResult result) {
        return Optional.ofNullable(insertedId(result))
                .map(ObjectId::toHexString)
                .orElse(null);
    }

    /**
     * Create a stream of entries from the given {@link FindIterable}. Using this method will create a stream that
     * properly closes the underlying MongoDB cursor when the stream is closed.
     * <p>
     * <b> The stream should be closed to free underlying resources.</b>
     *
     * @param findIterable The iterable to create the stream from.
     * @param <T>          document type of the underlying collection
     * @return A stream that should be used in a try-with-resources statement or closed manually to free underlying resources.
     */
    static <T> Stream<T> stream(@Nonnull FindIterable<T> findIterable) {
        final var cursor = findIterable.cursor();
        return Streams.stream(cursor).onClose(cursor::close);
    }

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
    Optional<T> getById(String id);

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
    boolean deleteById(String id);
}

