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
    <MET extends MongoEntity> TDocument getOrCreate(MET entity);

    @Nonnull
    String getCollectionName();

    @Nonnull
    MongoNamespace getNamespace();

    @Nonnull
    WriteConcern getWriteConcern();

    @Nonnull
    MongoCollection<TDocument> withWriteConcern(@Nonnull WriteConcern writeConcern);

    long countDocuments();

    long countDocuments(@Nonnull Bson bson);

    @Nonnull
    <TResult> DistinctIterable<TResult> distinct(@Nonnull String s, @Nonnull Class<TResult> aClass);

    @Nonnull
    <TResult> DistinctIterable<TResult> distinct(@Nonnull String s, @Nonnull Bson bson, @Nonnull Class<TResult> aClass);

    @Nonnull
    FindIterable<TDocument> find();

    @Nonnull
    FindIterable<TDocument> find(@Nonnull Bson bson);

    @Nonnull
    <TResult> FindIterable<TResult> find(@Nonnull Bson filter, @Nonnull Class<TResult> resultClass);

    @Nonnull
    AggregateIterable<TDocument> aggregate(@Nonnull List<? extends Bson> list);

    @Nonnull
    <TResult> AggregateIterable<TResult> aggregate(@Nonnull List<? extends Bson> list, @Nonnull Class<TResult> aClass);

    @Nonnull
    BulkWriteResult bulkWrite(@Nonnull List<? extends WriteModel<? extends TDocument>> list);

    @Nonnull
    InsertOneResult insertOne(@Nonnull TDocument entity);

    @Nonnull
    InsertManyResult insertMany(@Nonnull List<? extends TDocument> list);

    @Nonnull
    DeleteResult deleteOne(@Nonnull Bson bson);

    @Nonnull
    DeleteResult deleteMany(@Nonnull Bson bson);

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
    String createIndex(@Nonnull Bson bson);

    @Nonnull
    String createIndex(@Nonnull Bson bson, @Nonnull IndexOptions indexOptions);

    @Nonnull
    List<String> createIndexes(@Nonnull List<IndexModel> list);

    @Nonnull
    ListIndexesIterable<Document> listIndexes();

    void dropIndex(@Nonnull String indexName);

    void dropIndex(@Nonnull Bson bson);

    void dropIndexes();
}
