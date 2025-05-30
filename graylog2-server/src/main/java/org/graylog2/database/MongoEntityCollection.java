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
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.Document;
import org.bson.codecs.EncoderContext;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.graylog2.database.utils.MongoUtils.idEq;

/**
 * The default implementation of {@link MongoCollection}.
 * <p>
 * This class delegates most method calls to a {@link com.mongodb.client.MongoCollection} instance.
 */
public class MongoEntityCollection<T extends MongoEntity> implements MongoCollection<T> {
    private final com.mongodb.client.MongoCollection<T> delegate;

    public MongoEntityCollection(com.mongodb.client.MongoCollection<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public T getOrCreate(@Nonnull T entity) {
        requireNonNull(entity, "entity cannot be null");
        final var entityId = new ObjectId(requireNonNull(entity.id(), "entity ID cannot be null"));

        final var codec = delegate.getCodecRegistry().get(delegate.getDocumentClass());
        try (var writer = new BsonDocumentWriter(new BsonDocument())) {
            // Convert the DTO class to a Bson object, so we can use it with $setOnInsert
            codec.encode(writer, (T) entity, EncoderContext.builder().build());

            return delegate.findOneAndUpdate(
                    idEq(entityId),
                    Updates.setOnInsert(writer.getDocument()),
                    new FindOneAndUpdateOptions()
                            .returnDocument(ReturnDocument.AFTER)
                            .upsert(true)
            );
        }
    }

    @Nonnull
    @Override
    public MongoNamespace getNamespace() {
        return delegate.getNamespace();
    }

    @Nonnull
    @Override
    public WriteConcern getWriteConcern() {
        return delegate.getWriteConcern();
    }

    @Nonnull
    @Override
    public MongoCollection<T> withWriteConcern(@Nonnull WriteConcern writeConcern) {
        return new MongoEntityCollection<>(delegate.withWriteConcern(writeConcern));
    }

    @Override
    public long countDocuments() {
        return delegate.countDocuments();
    }

    @Override
    public long countDocuments(@Nonnull Bson filter) {
        return delegate.countDocuments(filter);
    }

    @Nonnull
    @Override
    public <TResult> DistinctIterable<TResult> distinct(@Nonnull String fieldName, @Nonnull Class<TResult> resultClass) {
        return delegate.distinct(fieldName, resultClass);
    }

    @Nonnull
    @Override
    public <TResult> DistinctIterable<TResult> distinct(@Nonnull String fieldName, @Nonnull Bson filter, @Nonnull Class<TResult> resultClass) {
        return delegate.distinct(fieldName, filter, resultClass);
    }

    @Nonnull
    @Override
    public FindIterable<T> find() {
        return delegate.find();
    }

    @Nonnull
    @Override
    public FindIterable<T> find(@Nonnull Bson filter) {
        return delegate.find(filter);
    }


    @Nonnull
    @Override
    public <TResult> FindIterable<TResult> find(@Nonnull Bson filter, @Nonnull Class<TResult> resultClass) {
        return delegate.find(filter, resultClass);
    }

    @Nonnull
    @Override
    public AggregateIterable<T> aggregate(@Nonnull List<? extends Bson> pipeline) {
        return delegate.aggregate(pipeline);
    }

    @Nonnull
    @Override
    public <TResult> AggregateIterable<TResult> aggregate(@Nonnull List<? extends Bson> pipeline, @Nonnull Class<TResult> resultClass) {
        return delegate.aggregate(pipeline, resultClass);
    }

    @Nonnull
    @Override
    public BulkWriteResult bulkWrite(@Nonnull List<? extends WriteModel<? extends T>> requests) {
        return delegate.bulkWrite(requests);
    }

    @Nonnull
    @Override
    public InsertOneResult insertOne(@Nonnull T entity) {
        return delegate.insertOne(entity);
    }

    @Nonnull
    @Override
    public InsertManyResult insertMany(@Nonnull List<? extends T> entities) {
        return delegate.insertMany(entities);
    }

    @Nonnull
    @Override
    public DeleteResult deleteOne(@Nonnull Bson filter) {
        return delegate.deleteOne(filter);
    }

    @Nonnull
    @Override
    public DeleteResult deleteMany(@Nonnull Bson filter) {
        return delegate.deleteMany(filter);
    }

    @Nonnull
    @Override
    public UpdateResult replaceOne(@Nonnull Bson filter, @Nonnull T replacement) {
        return delegate.replaceOne(filter, replacement);
    }

    @Nonnull
    @Override
    public UpdateResult replaceOne(@Nonnull Bson filter, @Nonnull T replacement, @Nonnull ReplaceOptions replaceOptions) {
        return delegate.replaceOne(filter, replacement, replaceOptions);
    }

    @Nonnull
    @Override
    public UpdateResult updateOne(@Nonnull Bson filter, @Nonnull Bson update) {
        return delegate.updateOne(filter, update);
    }

    @Nonnull
    @Override
    public UpdateResult updateOne(@Nonnull Bson filter, @Nonnull Bson update, @Nonnull UpdateOptions updateOptions) {
        return delegate.updateOne(filter, update, updateOptions);
    }


    @Nonnull
    @Override
    public UpdateResult updateMany(@Nonnull Bson filter, @Nonnull Bson update) {
        return delegate.updateMany(filter, update);
    }

    @Nonnull
    @Override
    public UpdateResult updateMany(@Nonnull Bson filter, @Nonnull Bson update, @Nonnull UpdateOptions updateOptions) {
        return delegate.updateMany(filter, update, updateOptions);
    }

    @Nullable
    @Override
    public T findOneAndDelete(@Nonnull Bson filter) {
        return delegate.findOneAndDelete(filter);
    }

    @Nullable
    @Override
    public T findOneAndReplace(@Nonnull Bson filter, @Nonnull T replacement, @Nonnull FindOneAndReplaceOptions options) {
        return delegate.findOneAndReplace(filter, replacement, options);
    }

    @Nullable
    @Override
    public T findOneAndUpdate(@Nonnull Bson filter, @Nonnull Bson update) {
        return delegate.findOneAndUpdate(filter, update);
    }

    @Nullable
    @Override
    public T findOneAndUpdate(@Nonnull Bson filter, @Nonnull Bson update, @Nonnull FindOneAndUpdateOptions options) {
        return delegate.findOneAndUpdate(filter, update, options);
    }

    @Override
    public void drop() {
        delegate.drop();
    }

    @Nonnull
    @Override
    public String createIndex(@Nonnull Bson keys) {
        return delegate.createIndex(keys);
    }

    @Nonnull
    @Override
    public String createIndex(@Nonnull Bson keys, @Nonnull IndexOptions indexOptions) {
        return delegate.createIndex(keys, indexOptions);
    }

    @Nonnull
    @Override
    public List<String> createIndexes(@Nonnull List<IndexModel> indexes) {
        return delegate.createIndexes(indexes);
    }

    @Nonnull
    @Override
    public ListIndexesIterable<Document> listIndexes() {
        return delegate.listIndexes();
    }

    @Override
    public void dropIndex(@Nonnull String indexName) {
        delegate.dropIndex(indexName);
    }

    @Override
    public void dropIndexes() {
        delegate.dropIndexes();
    }
}
