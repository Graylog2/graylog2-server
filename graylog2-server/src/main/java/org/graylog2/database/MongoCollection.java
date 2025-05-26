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

    @Nonnull
    MongoNamespace getNamespace();

    @Nonnull
    WriteConcern getWriteConcern();

    @Nonnull
    MongoCollection<TDocument> withWriteConcern(@Nonnull WriteConcern writeConcern);

    long countDocuments();

    long countDocuments(@Nonnull Bson bson);

    @Nonnull
    <TResult> DistinctIterable<TResult> distinct(@Nonnull String fieldName, @Nonnull Class<TResult> resultClass);

    @Nonnull
    <TResult> DistinctIterable<TResult> distinct(@Nonnull String fieldName, @Nonnull Bson filter, @Nonnull Class<TResult> resultClass);

    @Nonnull
    FindIterable<TDocument> find();

    @Nonnull
    FindIterable<TDocument> find(@Nonnull Bson filter);

    @Nonnull
    <TResult> FindIterable<TResult> find(@Nonnull Bson filter, @Nonnull Class<TResult> resultClass);

    @Nonnull
    AggregateIterable<TDocument> aggregate(@Nonnull List<? extends Bson> pipeline);

    @Nonnull
    <TResult> AggregateIterable<TResult> aggregate(@Nonnull List<? extends Bson> pipeline, @Nonnull Class<TResult> resultClass);

    @Nonnull
    BulkWriteResult bulkWrite(@Nonnull List<? extends WriteModel<? extends TDocument>> requests);

    @Nonnull
    InsertOneResult insertOne(@Nonnull TDocument entity);

    @Nonnull
    InsertManyResult insertMany(@Nonnull List<? extends TDocument> entities);

    @Nonnull
    DeleteResult deleteOne(@Nonnull Bson filter);

    @Nonnull
    DeleteResult deleteMany(@Nonnull Bson filter);

    @Nonnull
    UpdateResult replaceOne(@Nonnull Bson filter, @Nonnull TDocument entity);

    @Nonnull
    UpdateResult replaceOne(@Nonnull Bson filter, @Nonnull TDocument entity, @Nonnull ReplaceOptions options);

    @Nonnull
    UpdateResult updateOne(@Nonnull Bson filter, @Nonnull Bson updates);

    @Nonnull
    UpdateResult updateOne(@Nonnull Bson filter, @Nonnull Bson updates, @Nonnull UpdateOptions options);

    @Nonnull
    UpdateResult updateMany(@Nonnull Bson filter, @Nonnull Bson updates);

    @Nonnull
    UpdateResult updateMany(@Nonnull Bson filter, @Nonnull Bson updates, @Nonnull UpdateOptions options);

    TDocument findOneAndDelete(@Nonnull Bson filter);

    @Nullable
    TDocument findOneAndReplace(@Nonnull Bson filter, @Nonnull TDocument entity, @Nonnull FindOneAndReplaceOptions options);

    @Nullable
    TDocument findOneAndUpdate(@Nonnull Bson filter, @Nonnull Bson updates);

    @Nullable
    TDocument findOneAndUpdate(@Nonnull Bson filter, @Nonnull Bson updates, @Nonnull FindOneAndUpdateOptions options);

    void drop();

    @Nonnull
    String createIndex(@Nonnull Bson keys);

    @Nonnull
    String createIndex(@Nonnull Bson keys, @Nonnull IndexOptions indexOptions);

    @Nonnull
    List<String> createIndexes(@Nonnull List<IndexModel> indexes);

    @Nonnull
    ListIndexesIterable<Document> listIndexes();

    void dropIndex(@Nonnull String indexName);

    void dropIndex(@Nonnull Bson bson);

    void dropIndexes();
}
