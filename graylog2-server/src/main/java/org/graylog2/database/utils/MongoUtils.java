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
import com.google.common.collect.Streams;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.InsertOneResult;
import jakarta.annotation.Nonnull;
import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.BsonValue;
import org.bson.codecs.EncoderContext;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog2.database.BuildableMongoEntity;
import org.graylog2.database.MongoEntity;
import org.graylog2.database.jackson.CustomJacksonCodecRegistry;
import org.mongojack.InitializationRequiredForTransformation;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * Utility methods to interact with MongoDB collections of document types that extend {@link MongoEntity}. Some static
 * methods cannot enforce that type constraint but may fail if, if used with other document types.
 *
 * @param <T> Java type of the documents to interact with
 */
public class MongoUtils<T extends MongoEntity> {
    private final MongoCollection<T> collection;
    private final ObjectMapper objectMapper;
    private final CustomJacksonCodecRegistry codecRegistry;

    public MongoUtils(MongoCollection<T> delegate, ObjectMapper objectMapper) {
        this.collection = delegate;
        this.objectMapper = objectMapper;

        codecRegistry = new CustomJacksonCodecRegistry(
                objectMapper,
                collection.getCodecRegistry());
    }

    /**
     * Extract the inserted id of type {@link ObjectId} from the insert result.
     *
     * @param result Result object for inserting a document of type MongoEntity.
     * @return the inserted object ID. Fails if the id was not stored as an {@link ObjectId}.
     */
    public static ObjectId insertedId(@Nonnull InsertOneResult result) {
        final BsonValue insertedId = result.getInsertedId();
        if (insertedId == null) {
            // this should only happen when inserting RawBsonDocuments
            throw new IllegalArgumentException("Inserted ID is null. Make sure that you are inserting documents of " +
                    "type <? extends MongoEntity>.");
        }
        return insertedId.asObjectId().getValue();
    }

    /**
     * Extract the inserted id as a String from the insert result.
     *
     * @param result Result object for inserting a document of type MongoEntity.
     * @return the inserted object ID as string. Fails if the id was not stored as an {@link ObjectId}.
     */
    public static String insertedIdAsString(@Nonnull InsertOneResult result) {
        return insertedId(result).toHexString();
    }

    /**
     * Create a query constraint to match a document's ID against the given Hex string.
     *
     * @param id Hex string representation of an {@link ObjectId}
     * @return An 'eq' filter.
     */
    public static Bson idEq(@Nonnull String id) {
        return idEq(new ObjectId(id));
    }

    /**
     * Create a query constraint to match a document's ID.
     *
     * @param id ID to match
     * @return An 'eq' filter.
     */
    public static Bson idEq(@Nonnull ObjectId id) {
        return Filters.eq("_id", id);
    }

    /**
     * Create a query constraint to match a field against an {@link ObjectId}.
     *
     * @param fieldName the field to check
     * @param id        the String value of the ObjectId
     * @return the filter
     */
    public static Bson objectIdEq(@Nonnull String fieldName, @Nonnull String id) {
        return objectIdEq(fieldName, new ObjectId(id));
    }

    /**
     * Create a query constraint to match a field against an {@link ObjectId}.
     *
     * @param fieldName the field to check
     * @param objectId  the ObjectId
     * @return the filter
     */
    public static Bson objectIdEq(@Nonnull String fieldName, @Nonnull ObjectId objectId) {
        return Filters.eq(fieldName, objectId);
    }

    /**
     * Create a query constraint to match a document's ID against the given list of Hex strings.
     *
     * @param ids Collection of hex string representations of an {@link ObjectId}
     * @return An 'in' filter.
     */
    public static Bson stringIdsIn(Collection<String> ids) {
        return idsIn(ids.stream().map(ObjectId::new).collect(Collectors.toSet()));
    }

    /**
     * Create a query constraint to match a document's ID against a list of IDs.
     *
     * @param ids IDs to match
     * @return An 'in' filter.
     */
    public static Bson idsIn(Collection<ObjectId> ids) {
        return Filters.in("_id", ids);
    }

    /**
     * Create a stream of entries from the given {@link MongoIterable}. Using this method will create a stream that
     * properly closes the underlying MongoDB cursor when the stream is closed.
     * <p>
     * <b> The stream should be closed to free underlying resources.</b>
     *
     * @param mongoIterable The iterable to create the stream from.
     * @param <T>           document type of the underlying collection
     * @return A stream that should be used in a try-with-resources statement or closed manually to free underlying resources.
     */
    public static <T> Stream<T> stream(@Nonnull MongoIterable<T> mongoIterable) {
        final var cursor = mongoIterable.cursor();
        return Streams.stream(cursor).onClose(cursor::close);
    }

    /**
     * Checks if the given {@link MongoException} represents a duplicate key error by checking its error code.
     *
     * @param e Exception that has been thrown by a MongoDB operation
     * @return true if the exception represents a duplicate key error, false otherwise
     */
    public static boolean isDuplicateKeyError(MongoException e) {
        return ErrorCategory.fromErrorCode(e.getCode()) == ErrorCategory.DUPLICATE_KEY;
    }

    /**
     * Convenience method to look up a single document by its ID.
     *
     * @param id the document's id.
     * @return the document wrapped in an {@link Optional} if present in the DB, an empty {@link Optional} otherwise.
     */
    public Optional<T> getById(ObjectId id) {
        return Optional.ofNullable(collection.find(idEq(id)).first());
    }

    /**
     * Convenience method to look up a single document by its ID.
     *
     * @param id Hex string representation of the document's {@link ObjectId}.
     * @return the document wrapped in an {@link Optional} if present in the DB, an empty {@link Optional} otherwise.
     */
    public Optional<T> getById(String id) {
        return getById(new ObjectId(id));
    }

    /**
     * Convenience method to delete a single document identified by its ID.
     *
     * @param id the document's id.
     * @return true if a document was deleted, false otherwise.
     */
    public boolean deleteById(ObjectId id) {
        return collection.deleteOne(idEq(id)).getDeletedCount() > 0;
    }

    /**
     * Convenience method to delete a single document identified by its ID.
     *
     * @param id Hex string representation of the document's {@link ObjectId}.
     * @return true if a document was deleted, false otherwise.
     */
    public boolean deleteById(String id) {
        return deleteById(new ObjectId(id));
    }

    /**
     * Convenience method to atomically get or create the given entity. If the collection doesn't contain an entity
     * with the entity's ID, it will be created and returned. If the entity exists, the method returns the unmodified
     * entity from the collection.
     * <p>
     * The entity's ID must not be null!
     *
     * @param entity the entity to
     * @return the existing or newly created entity
     * @throws NullPointerException when the entity or entity ID is null
     */
    public T getOrCreate(T entity) {
        requireNonNull(entity, "entity cannot be null");
        final var entityId = new ObjectId(requireNonNull(entity.id(), "entity ID cannot be null"));

        final var codec = collection.getCodecRegistry().get(collection.getDocumentClass());
        try (var writer = new BsonDocumentWriter(new BsonDocument())) {
            // Convert the DTO class to a Bson object, so we can use it with $setOnInsert
            codec.encode(writer, entity, EncoderContext.builder().build());

            return collection.findOneAndUpdate(
                    idEq(entityId),
                    Updates.setOnInsert(writer.getDocument()),
                    new FindOneAndUpdateOptions()
                            .returnDocument(ReturnDocument.AFTER)
                            .upsert(true)
            );
        }
    }

    /**
     * Saves an entity by either inserting or replacing the document.
     * <p>
     * This method exists to avoid the repeated implementation of this functionality during migration from the old
     * Mongojack API.
     * <p>
     * <b> For new code, prefer implementing a separate "create" and "update" path instead.</b>
     *
     * @param entity Entity to be saved, with the #id() property optionally set.
     * @return Saved entity with the #id() property guaranteed to be present.
     */
    public T save(BuildableMongoEntity<T, ?> entity) {
        // going through the builder is a bit more work but avoids an unsafe cast to T
        final var orig = entity.toBuilder().build();
        final var id = orig.id();
        if (id == null) {
            final var insertedId = insertedIdAsString(collection.insertOne(orig));
            return entity.toBuilder().id(insertedId).build();
        } else {
            collection.replaceOne(idEq(id), orig, new ReplaceOptions().upsert(true));
            return orig;
        }
    }

    /**
     * Utility method to help moving away from the deprecated MongoJack Bson objects, like
     * {@link org.mongojack.DBQuery.Query}. These objects require initialization before they can be used as regular
     * {@link org.bson.conversions.Bson} objects with the MongoDB driver.
     * <p>
     * The {@link org.mongojack.JacksonMongoCollection} would usually take care of that, but because we cannot use it,
     * and instead use a regular {@link org.mongojack.MongoCollection}, we have to use this method.
     *
     * @deprecated This method is only meant as an interim solution. Rewrite your deprecated MongoJack objects so that
     * you don't have to use it.
     */
    @Deprecated
    public void initializeLegacyMongoJackBsonObject(InitializationRequiredForTransformation mongoJackBsonObject) {
        mongoJackBsonObject.initialize(
                objectMapper,
                objectMapper.constructType(collection.getDocumentClass()),
                codecRegistry);
    }

}
