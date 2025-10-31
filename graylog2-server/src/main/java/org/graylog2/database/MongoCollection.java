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

import com.mongodb.MongoNamespace;
import com.mongodb.WriteConcern;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.List;

/**
 * A custom MongoDB collection interface for entities that implement {@link MongoEntity}.
 * <p>
 * This interface provides methods for common database operations such as finding, inserting, updating, and deleting
 * documents, as well as managing indexes.
 * <p>
 * The interface mirrors the methods of the MongoDB {@link com.mongodb.client.MongoCollection MongoCollection}
 * interface, but is tailored for entities that implement {@link MongoEntity}. It allows for more control over the
 * mutations of entities in the database.
 *
 * @param <TDocument> the type of the document stored in this collection, which must extend {@link MongoEntity}
 */
public interface MongoCollection<TDocument extends MongoEntity> {
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
    TDocument getOrCreate(TDocument entity);

    /**
     * Gets the namespace of this collection.
     *
     * @return the namespace
     */
    @Nonnull
    MongoNamespace getNamespace();

    /**
     * Get the write concern for the MongoCollection.
     *
     * @return the {@link com.mongodb.WriteConcern}
     */
    @Nonnull
    WriteConcern getWriteConcern();

    /**
     * Create a new MongoCollection instance with a different write concern.
     *
     * @param writeConcern the new {@link com.mongodb.WriteConcern} for the collection
     * @return a new MongoCollection instance with the different writeConcern
     */
    @Nonnull
    MongoCollection<TDocument> withWriteConcern(@Nonnull WriteConcern writeConcern);

    /**
     * Counts the number of documents in the collection.
     *
     * <p>
     * Note: For a fast count of the total documents in a collection see {@link #estimatedDocumentCount()}.
     * When migrating from {@code count()} to {@code countDocuments()} the following query operators must be replaced:
     * </p>
     * <pre>
     *
     *  +-------------+--------------------------------+
     *  | Operator    | Replacement                    |
     *  +=============+================================+
     *  | $where      |  $expr                         |
     *  +-------------+--------------------------------+
     *  | $near       |  $geoWithin with $center       |
     *  +-------------+--------------------------------+
     *  | $nearSphere |  $geoWithin with $centerSphere |
     *  +-------------+--------------------------------+
     * </pre>
     *
     * @return the number of documents in the collection
     */
    long countDocuments();

    /**
     * Counts the number of documents in the collection according to the given options.
     *
     * <p>
     * Note: For a fast count of the total documents in a collection see {@link #estimatedDocumentCount()}.
     * When migrating from {@code count()} to {@code countDocuments()} the following query operators must be replaced:
     * </p>
     * <pre>
     *
     *  +-------------+--------------------------------+
     *  | Operator    | Replacement                    |
     *  +=============+================================+
     *  | $where      |  $expr                         |
     *  +-------------+--------------------------------+
     *  | $near       |  $geoWithin with $center       |
     *  +-------------+--------------------------------+
     *  | $nearSphere |  $geoWithin with $centerSphere |
     *  +-------------+--------------------------------+
     * </pre>
     *
     * @param filter the query filter
     * @return the number of documents in the collection
     * @since 3.8
     */
    long countDocuments(@Nonnull Bson filter);

    /**
     * Gets the distinct values of the specified field name.
     *
     * @param fieldName   the field name
     * @param resultClass the class to cast any distinct items into.
     * @param <TResult>   the target type of the iterable.
     * @return an iterable of distinct values
     */
    @Nonnull
    <TResult> DistinctIterable<TResult> distinct(@Nonnull String fieldName, @Nonnull Class<TResult> resultClass);

    /**
     * Gets the distinct values of the specified field name.
     *
     * @param fieldName   the field name
     * @param filter      the query filter
     * @param resultClass the class to cast any distinct items into.
     * @param <TResult>   the target type of the iterable.
     * @return an iterable of distinct values
     */
    @Nonnull
    <TResult> DistinctIterable<TResult> distinct(@Nonnull String fieldName, @Nonnull Bson filter, @Nonnull Class<TResult> resultClass);

    /**
     * Finds all documents in the collection.
     *
     * @return the find iterable interface
     */
    @Nonnull
    FindIterable<TDocument> find();

    /**
     * Finds all documents in the collection.
     *
     * @param filter the query filter
     * @return the find iterable interface
     */
    @Nonnull
    FindIterable<TDocument> find(@Nonnull Bson filter);

    /**
     * Finds all documents in the collection.
     *
     * @param filter      the query filter
     * @param resultClass the class to decode each document into
     * @param <TResult>   the target document type of the iterable.
     * @return the find iterable interface
     */
    @Nonnull
    <TResult> FindIterable<TResult> find(@Nonnull Bson filter, @Nonnull Class<TResult> resultClass);

    /**
     * Aggregates documents according to the specified aggregation pipeline.
     *
     * @param pipeline the aggregation pipeline
     * @return an iterable containing the result of the aggregation operation
     */
    @Nonnull
    AggregateIterable<TDocument> aggregate(@Nonnull List<? extends Bson> pipeline);

    /**
     * Aggregates documents according to the specified aggregation pipeline.
     *
     * @param pipeline    the aggregation pipeline
     * @param resultClass the class to decode each document into
     * @param <TResult>   the target document type of the iterable.
     * @return an iterable containing the result of the aggregation operation
     */
    @Nonnull
    <TResult> AggregateIterable<TResult> aggregate(@Nonnull List<? extends Bson> pipeline, @Nonnull Class<TResult> resultClass);

    /**
     * Executes a mix of inserts, updates, replaces, and deletes.
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.
     * The eligibility for retryable write support for bulk operations is determined on the whole bulk write. If the {@code requests}
     * contain any {@code UpdateManyModels} or {@code DeleteManyModels} then the bulk operation will not support retryable writes.</p>
     *
     * @param requests the writes to execute
     * @return the result of the bulk write
     * @throws com.mongodb.MongoBulkWriteException if there's an exception in the bulk write operation
     * @throws com.mongodb.MongoException          if there's an exception running the operation
     */
    @Nonnull
    BulkWriteResult bulkWrite(@Nonnull List<? extends WriteModel<? extends TDocument>> requests);

    /**
     * Inserts the provided document. If the document is missing an identifier, the driver should generate one.
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param entity the entity to insert
     * @return the insert one result
     * @throws com.mongodb.MongoWriteException        if the write failed due to some specific write exception
     * @throws com.mongodb.MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws com.mongodb.MongoCommandException      if the write failed due to a specific command exception
     * @throws com.mongodb.MongoException             if the write failed due some other failure
     */
    @Nonnull
    InsertOneResult insertOne(@Nonnull TDocument entity);

    /**
     * Inserts one or more documents.  A call to this method is equivalent to a call to the {@code bulkWrite} method
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param entities the entities to insert
     * @return the insert many result
     * @throws com.mongodb.MongoBulkWriteException if there's an exception in the bulk write operation
     * @throws com.mongodb.MongoCommandException   if the write failed due to a specific command exception
     * @throws com.mongodb.MongoException          if the write failed due some other failure
     * @throws IllegalArgumentException            if the documents list is null or empty, or any of the documents in the list are null
     * @see com.mongodb.client.MongoCollection#bulkWrite
     */
    @Nonnull
    InsertManyResult insertMany(@Nonnull List<? extends TDocument> entities);

    /**
     * Removes at most one document from the collection that matches the given filter.  If no documents match, the collection is not
     * modified.
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param filter the query filter to apply the delete operation
     * @return the result of the remove one operation
     * @throws com.mongodb.MongoWriteException        if the write failed due to some specific write exception
     * @throws com.mongodb.MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws com.mongodb.MongoCommandException      if the write failed due to a specific command exception
     * @throws com.mongodb.MongoException             if the write failed due some other failure
     */
    @Nonnull
    DeleteResult deleteOne(@Nonnull Bson filter);

    /**
     * Removes all documents from the collection that match the given query filter.  If no documents match, the collection is not modified.
     *
     * @param filter the query filter to apply the delete operation
     * @return the result of the remove many operation
     * @throws com.mongodb.MongoWriteException        if the write failed due to some specific write exception
     * @throws com.mongodb.MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws com.mongodb.MongoCommandException      if the write failed due to a specific command exception
     * @throws com.mongodb.MongoException             if the write failed due some other failure
     */
    @Nonnull
    DeleteResult deleteMany(@Nonnull Bson filter);

    /**
     * Replace a document in the collection according to the specified arguments.
     *
     * <p>Use this method to replace a document using the specified replacement argument. To update the document with update operators, use
     * the corresponding {@link #updateOne(Bson, Bson)} method.</p>
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param filter      the query filter to apply the replace operation
     * @param replacement the replacement document
     * @return the result of the replace one operation
     * @throws com.mongodb.MongoWriteException        if the write failed due to some specific write exception
     * @throws com.mongodb.MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws com.mongodb.MongoCommandException      if the write failed due to a specific command exception
     * @throws com.mongodb.MongoException             if the write failed due some other failure
     */
    @Nonnull
    UpdateResult replaceOne(@Nonnull Bson filter, @Nonnull TDocument replacement);

    /**
     * Replace a document in the collection according to the specified arguments.
     *
     * <p>Use this method to replace a document using the specified replacement argument. To update the document with update operators, use
     * the corresponding {@link #updateOne(Bson, Bson, UpdateOptions)} method.</p>
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param filter         the query filter to apply the replace operation
     * @param replacement    the replacement document
     * @param replaceOptions the options to apply to the replace operation
     * @return the result of the replace one operation
     * @throws com.mongodb.MongoWriteException        if the write failed due to some specific write exception
     * @throws com.mongodb.MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws com.mongodb.MongoCommandException      if the write failed due to a specific command exception
     * @throws com.mongodb.MongoException             if the write failed due some other failure
     * @since 3.7
     */
    @Nonnull
    UpdateResult replaceOne(@Nonnull Bson filter, @Nonnull TDocument replacement, @Nonnull ReplaceOptions replaceOptions);

    /**
     * Update a single document in the collection according to the specified arguments.
     *
     * <p>Use this method to only update the corresponding fields in the document according to the update operators used in the update
     * document. To replace the entire document with a new document, use the corresponding {@link #replaceOne(Bson, MongoEntity)} method.</p>
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param filter a document describing the query filter, which may not be null.
     * @param update a document describing the update, which may not be null. The update to apply must include at least one update operator.
     * @return the result of the update one operation
     * @throws com.mongodb.MongoWriteException        if the write failed due to some specific write exception
     * @throws com.mongodb.MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws com.mongodb.MongoCommandException      if the write failed due to a specific command exception
     * @throws com.mongodb.MongoException             if the write failed due some other failure
     * @see com.mongodb.client.MongoCollection#replaceOne(Bson, Object)
     */
    @Nonnull
    UpdateResult updateOne(@Nonnull Bson filter, @Nonnull Bson update);

    /**
     * Update a single document in the collection according to the specified arguments.
     *
     * <p>Use this method to only update the corresponding fields in the document according to the update operators used in the update
     * document. To replace the entire document with a new document, use the corresponding {@link #replaceOne(Bson, MongoEntity, ReplaceOptions)}
     * method.</p>
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param filter        a document describing the query filter, which may not be null.
     * @param update        a document describing the update, which may not be null. The update to apply must include at least one update
     *                      operator.
     * @param updateOptions the options to apply to the update operation
     * @return the result of the update one operation
     * @throws com.mongodb.MongoWriteException        if the write failed due to some specific write exception
     * @throws com.mongodb.MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws com.mongodb.MongoCommandException      if the write failed due to a specific command exception
     * @throws com.mongodb.MongoException             if the write failed due some other failure
     * @see com.mongodb.client.MongoCollection#replaceOne(Bson, Object, ReplaceOptions)
     */
    @Nonnull
    UpdateResult updateOne(@Nonnull Bson filter, @Nonnull Bson update, @Nonnull UpdateOptions updateOptions);

    /**
     * Update all documents in the collection according to the specified arguments.
     *
     * @param filter a document describing the query filter, which may not be null.
     * @param update a document describing the update, which may not be null. The update to apply must include only update operators.
     * @return the result of the update many operation
     * @throws com.mongodb.MongoWriteException        if the write failed due to some specific write exception
     * @throws com.mongodb.MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws com.mongodb.MongoCommandException      if the write failed due to a specific command exception
     * @throws com.mongodb.MongoException             if the write failed due some other failure
     */
    @Nonnull
    UpdateResult updateMany(@Nonnull Bson filter, @Nonnull Bson update);

    /**
     * Update all documents in the collection according to the specified arguments.
     *
     * @param filter        a document describing the query filter, which may not be null.
     * @param update        a document describing the update, which may not be null. The update to apply must include only update operators.
     * @param updateOptions the options to apply to the update operation
     * @return the result of the update many operation
     * @throws com.mongodb.MongoWriteException        if the write failed due to some specific write exception
     * @throws com.mongodb.MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws com.mongodb.MongoCommandException      if the write failed due to a specific command exception
     * @throws com.mongodb.MongoException             if the write failed due some other failure
     */
    @Nonnull
    UpdateResult updateMany(@Nonnull Bson filter, @Nonnull Bson update, @Nonnull UpdateOptions updateOptions);

    /**
     * Atomically find a document and remove it.
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param filter the query filter to find the document with
     * @return the document that was removed.  If no documents matched the query filter, then null will be returned
     */
    @Nullable
    TDocument findOneAndDelete(@Nonnull Bson filter);

    /**
     * Atomically find a document and replace it.
     *
     * <p>Use this method to replace a document using the specified replacement argument. To update the document with update operators, use
     * the corresponding {@link #findOneAndUpdate(Bson, Bson)} method.</p>
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param filter      the query filter to apply the replace operation
     * @param replacement the replacement document
     * @return the document that was replaced.  Depending on the value of the {@code returnOriginal} property, this will either be the
     * document as it was before the update or as it is after the update.  If no documents matched the query filter, then null will be
     * returned
     */
    @Nullable
    TDocument findOneAndReplace(@Nonnull Bson filter, @Nonnull TDocument replacement, @Nonnull FindOneAndReplaceOptions options);

    /**
     * Atomically find a document and update it.
     *
     * <p>Use this method to only update the corresponding fields in the document according to the update operators used in the update
     * document. To replace the entire document with a new document, use the corresponding {@link #findOneAndReplace(Bson, MongoEntity, FindOneAndReplaceOptions)}.
     * </p>
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param filter a document describing the query filter, which may not be null.
     * @param update a document describing the update, which may not be null. The update to apply must include at least one update operator.
     * @return the document that was updated before the update was applied.  If no documents matched the query filter, then null will be
     * returned
     * @see com.mongodb.client.MongoCollection#findOneAndReplace(Bson, Object)
     */
    @Nullable
    TDocument findOneAndUpdate(@Nonnull Bson filter, @Nonnull Bson update);

    /**
     * Atomically find a document and update it.
     *
     * <p>Use this method to only update the corresponding fields in the document according to the update operators used in the update
     * document. To replace the entire document with a new document, use the corresponding {@link #findOneAndReplace(Bson, MongoEntity, FindOneAndReplaceOptions)} method.</p>
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param filter  a document describing the query filter, which may not be null.
     * @param update  a document describing the update, which may not be null. The update to apply must include at least one update
     *                operator.
     * @param options the options to apply to the operation
     * @return the document that was updated.  Depending on the value of the {@code returnOriginal} property, this will either be the
     * document as it was before the update or as it is after the update.  If no documents matched the query filter, then null will be
     * returned
     * @see com.mongodb.client.MongoCollection#findOneAndReplace(Bson, Object, FindOneAndReplaceOptions)
     */
    @Nullable
    TDocument findOneAndUpdate(@Nonnull Bson filter, @Nonnull Bson update, @Nonnull FindOneAndUpdateOptions options);

    /**
     * Atomically find a document and update it.
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param filter  a document describing the query filter, which may not be null.
     * @param update  a pipeline describing the update, which may not be null.
     * @param options the options to apply to the operation
     * @return the document that was updated.  Depending on the value of the {@code returnOriginal} property, this will either be the
     * document as it was before the update or as it is after the update.  If no documents matched the query filter, then null will be
     * returned
     */
    @Nullable
    TDocument findOneAndUpdate(@Nonnull Bson filter, @Nonnull List<? extends Bson> update, @Nonnull FindOneAndUpdateOptions options);

    /**
     * Drops this collection from the Database.
     */
    void drop();

    /**
     * Create an index with the given keys.
     *
     * @param keys an object describing the index key(s), which may not be null.
     * @return the index name
     */
    @Nonnull
    String createIndex(@Nonnull Bson keys);

    /**
     * Create an index with the given keys and options.
     *
     * @param keys         an object describing the index key(s), which may not be null.
     * @param indexOptions the options for the index
     * @return the index name
     */
    @Nonnull
    String createIndex(@Nonnull Bson keys, @Nonnull IndexOptions indexOptions);

    /**
     * Create multiple indexes.
     *
     * @param indexes the list of indexes
     * @return the list of index names
     */
    @Nonnull
    List<String> createIndexes(@Nonnull List<IndexModel> indexes);

    /**
     * Get all the indexes in this collection.
     *
     * @return the list indexes iterable interface
     */
    @Nonnull
    ListIndexesIterable<Document> listIndexes();

    /**
     * Drops the index given its name.
     *
     * @param indexName the name of the index to remove
     */
    void dropIndex(@Nonnull String indexName);

    /**
     * Drop all the indexes on this collection, except for the default on _id.
     */
    void dropIndexes();
}
