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

//    @Nonnull
//    @Override
//    public Class<T> getDocumentClass() {
//        return delegate.getDocumentClass();
//    }
//
//    @Nonnull
//    @Override
//    public CodecRegistry getCodecRegistry() {
//        return delegate.getCodecRegistry();
//    }
//
//    @Nonnull
//    @Override
//    public ReadPreference getReadPreference() {
//        return delegate.getReadPreference();
//    }

    @Nonnull
    @Override
    public WriteConcern getWriteConcern() {
        return delegate.getWriteConcern();
    }

//    @Nonnull
//    @Override
//    public ReadConcern getReadConcern() {
//        return delegate.getReadConcern();
//    }
//
//    @Nullable
//    @Alpha({Reason.CLIENT})
//    @Override
//    public Long getTimeout(@Nonnull TimeUnit timeUnit) {
//        return delegate.getTimeout(timeUnit);
//    }
//
//    @Nonnull
//    @Override
//    public <NewTDocument> MongoCollection<NewTDocument> withDocumentClass(@Nonnull Class<NewTDocument> aClass) {
//        return delegate.withDocumentClass(aClass);
//    }
//
//    @Nonnull
//    @Override
//    public MongoCollection<T> withCodecRegistry(@Nonnull CodecRegistry codecRegistry) {
//        return new GraylogMongoCollection<>(delegate.withCodecRegistry(codecRegistry));
//    }
//
//    @Nonnull
//    @Override
//    public MongoCollection<T> withReadPreference(@Nonnull ReadPreference readPreference) {
//        return new GraylogMongoCollection<>(delegate.withReadPreference(readPreference));
//    }

    @Nonnull
    @Override
    public MongoCollection<T> withWriteConcern(@Nonnull WriteConcern writeConcern) {
        return new MongoEntityCollection<>(delegate.withWriteConcern(writeConcern));
    }

//    @Nonnull
//    @Override
//    public MongoCollection<T> withReadConcern(@Nonnull ReadConcern readConcern) {
//        return new GraylogMongoCollection<>(delegate.withReadConcern(readConcern));
//    }
//
//    @Nonnull
//    @Alpha({Reason.CLIENT})
//    @Override
//    public MongoCollection<T> withTimeout(long l, @Nonnull TimeUnit timeUnit) {
//        return new GraylogMongoCollection<>(delegate.withTimeout(l, timeUnit));
//    }

    @Override
    public long countDocuments() {
        return delegate.countDocuments();
    }

    @Override
    public long countDocuments(@Nonnull Bson bson) {
        return delegate.countDocuments(bson);
    }

    //    @Override
//    public long countDocuments(@Nonnull Bson bson, @Nonnull CountOptions countOptions) {
//        return delegate.countDocuments(bson, countOptions);
//    }
//
//    @Override
//    public long countDocuments(@Nonnull ClientSession clientSession) {
//        return delegate.countDocuments(clientSession);
//    }
//
//    @Override
//    public long countDocuments(@Nonnull ClientSession clientSession, @Nonnull Bson bson) {
//        return delegate.countDocuments(clientSession, bson);
//    }
//
//    @Override
//    public long countDocuments(@Nonnull ClientSession clientSession, @Nonnull Bson bson, @Nonnull CountOptions countOptions) {
//        return delegate.countDocuments(clientSession, bson, countOptions);
//    }
//
//    @Override
//    public long estimatedDocumentCount() {
//        return delegate.estimatedDocumentCount();
//    }
//
//    @Override
//    public long estimatedDocumentCount(@Nonnull EstimatedDocumentCountOptions estimatedDocumentCountOptions) {
//        return delegate.estimatedDocumentCount(estimatedDocumentCountOptions);
//    }

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

//    @Nonnull
//    @Override
//    public <TResult> DistinctIterable<TResult> distinct(@Nonnull ClientSession clientSession, @Nonnull String s, @Nonnull Class<TResult> aClass) {
//        return delegate.distinct(clientSession, s, aClass);
//    }
//
//    @Nonnull
//    @Override
//    public <TResult> DistinctIterable<TResult> distinct(@Nonnull ClientSession clientSession, @Nonnull String s, @Nonnull Bson bson, @Nonnull Class<TResult> aClass) {
//        return delegate.distinct(clientSession, s, bson, aClass);
//    }
//
    @Nonnull
    @Override
    public FindIterable<T> find() {
        return delegate.find();
    }

    //    @Nonnull
//    @Override
//    public <TResult> FindIterable<TResult> find(@Nonnull Class<TResult> aClass) {
//        return delegate.find(aClass);
//    }
//
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

//    @Nonnull
//    @Override
//    public FindIterable<T> find(@Nonnull ClientSession clientSession) {
//        return delegate.find(clientSession);
//    }
//
//    @Nonnull
//    @Override
//    public <TResult> FindIterable<TResult> find(@Nonnull ClientSession clientSession, @Nonnull Class<TResult> aClass) {
//        return delegate.find(clientSession, aClass);
//    }
//
//    @Nonnull
//    @Override
//    public FindIterable<T> find(@Nonnull ClientSession clientSession, @Nonnull Bson bson) {
//        return delegate.find(clientSession, bson);
//    }
//
//    @Nonnull
//    @Override
//    public <TResult> FindIterable<TResult> find(@Nonnull ClientSession clientSession, @Nonnull Bson bson, @Nonnull Class<TResult> aClass) {
//        return delegate.find(clientSession, bson, aClass);
//    }

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

//    @Nonnull
//    @Override
//    public AggregateIterable<T> aggregate(@Nonnull ClientSession clientSession, @Nonnull List<? extends Bson> list) {
//        return delegate.aggregate(clientSession, list);
//    }
//
//    @Nonnull
//    @Override
//    public <TResult> AggregateIterable<TResult> aggregate(@Nonnull ClientSession clientSession, @Nonnull List<? extends Bson> list, @Nonnull Class<TResult> aClass) {
//        return delegate.aggregate(clientSession, list, aClass);
//    }
//
//    @Nonnull
//    @Override
//    public ChangeStreamIterable<T> watch() {
//        return delegate.watch();
//    }
//
//    @Nonnull
//    @Override
//    public <TResult> ChangeStreamIterable<TResult> watch(@Nonnull Class<TResult> aClass) {
//        return delegate.watch(aClass);
//    }
//
//    @Nonnull
//    @Override
//    public ChangeStreamIterable<T> watch(@Nonnull List<? extends Bson> list) {
//        return delegate.watch(list);
//    }
//
//    @Nonnull
//    @Override
//    public <TResult> ChangeStreamIterable<TResult> watch(@Nonnull List<? extends Bson> list, @Nonnull Class<TResult> aClass) {
//        return delegate.watch(list, aClass);
//    }
//
//    @Nonnull
//    @Override
//    public ChangeStreamIterable<T> watch(@Nonnull ClientSession clientSession) {
//        return delegate.watch(clientSession);
//    }
//
//    @Nonnull
//    @Override
//    public <TResult> ChangeStreamIterable<TResult> watch(@Nonnull ClientSession clientSession, @Nonnull Class<TResult> aClass) {
//        return delegate.watch(clientSession, aClass);
//    }
//
//    @Nonnull
//    @Override
//    public ChangeStreamIterable<T> watch(@Nonnull ClientSession clientSession, @Nonnull List<? extends Bson> list) {
//        return delegate.watch(clientSession, list);
//    }
//
//    @Nonnull
//    @Override
//    public <TResult> ChangeStreamIterable<TResult> watch(@Nonnull ClientSession clientSession, @Nonnull List<? extends Bson> list, @Nonnull Class<TResult> aClass) {
//        return delegate.watch(clientSession, list, aClass);
//    }
//
//    @Nonnull
//    @Deprecated
//    @Override
//    public MapReduceIterable<T> mapReduce(@Nonnull String s, @Nonnull String s1) {
//        return delegate.mapReduce(s, s1);
//    }
//
//    @Nonnull
//    @Deprecated
//    @Override
//    public <TResult> MapReduceIterable<TResult> mapReduce(@Nonnull String s, @Nonnull String s1, @Nonnull Class<TResult> aClass) {
//        return delegate.mapReduce(s, s1, aClass);
//    }
//
//    @Nonnull
//    @Deprecated
//    @Override
//    public MapReduceIterable<T> mapReduce(@Nonnull ClientSession clientSession, @Nonnull String s, @Nonnull String s1) {
//        return delegate.mapReduce(clientSession, s, s1);
//    }
//
//    @Nonnull
//    @Deprecated
//    @Override
//    public <TResult> MapReduceIterable<TResult> mapReduce(@Nonnull ClientSession clientSession, @Nonnull String s, @Nonnull String s1, @Nonnull Class<TResult> aClass) {
//        return delegate.mapReduce(clientSession, s, s1, aClass);
//    }

    @Nonnull
    @Override
    public BulkWriteResult bulkWrite(@Nonnull List<? extends WriteModel<? extends T>> requests) {
        return delegate.bulkWrite(requests);
    }

//    @Nonnull
//    @Override
//    public BulkWriteResult bulkWrite(@Nonnull List<? extends WriteModel<? extends T>> list, @Nonnull BulkWriteOptions bulkWriteOptions) {
//        return delegate.bulkWrite(list, bulkWriteOptions);
//    }
//
//    @Nonnull
//    @Override
//    public BulkWriteResult bulkWrite(@Nonnull ClientSession clientSession, @Nonnull List<? extends WriteModel<? extends T>> list) {
//        return delegate.bulkWrite(clientSession, list);
//    }
//
//    @Nonnull
//    @Override
//    public BulkWriteResult bulkWrite(@Nonnull ClientSession clientSession, @Nonnull List<? extends WriteModel<? extends T>> list, @Nonnull BulkWriteOptions bulkWriteOptions) {
//        return delegate.bulkWrite(clientSession, list, bulkWriteOptions);
//    }

    @Nonnull
    @Override
    public InsertOneResult insertOne(@Nonnull T entity) {
        return delegate.insertOne(entity);
    }

//    @Nonnull
//    @Override
//    public InsertOneResult insertOne(@Nonnull T t, @Nonnull InsertOneOptions insertOneOptions) {
//        return delegate.insertOne(t, insertOneOptions);
//    }
//
//    @Nonnull
//    @Override
//    public InsertOneResult insertOne(@Nonnull ClientSession clientSession, @Nonnull T t) {
//        return delegate.insertOne(clientSession, t);
//    }
//
//    @Nonnull
//    @Override
//    public InsertOneResult insertOne(@Nonnull ClientSession clientSession, @Nonnull T t, @Nonnull InsertOneOptions insertOneOptions) {
//        return delegate.insertOne(clientSession, t, insertOneOptions);
//    }

    @Nonnull
    @Override
    public InsertManyResult insertMany(@Nonnull List<? extends T> entities) {
        return delegate.insertMany(entities);
    }

//    @Nonnull
//    @Override
//    public InsertManyResult insertMany(@Nonnull List<? extends T> list, @Nonnull InsertManyOptions insertManyOptions) {
//        return delegate.insertMany(list, insertManyOptions);
//    }
//
//    @Nonnull
//    @Override
//    public InsertManyResult insertMany(@Nonnull ClientSession clientSession, @Nonnull List<? extends T> list) {
//        return delegate.insertMany(clientSession, list);
//    }
//
//    @Nonnull
//    @Override
//    public InsertManyResult insertMany(@Nonnull ClientSession clientSession, @Nonnull List<? extends T> list, @Nonnull InsertManyOptions insertManyOptions) {
//        return delegate.insertMany(clientSession, list, insertManyOptions);
//    }

    @Nonnull
    @Override
    public DeleteResult deleteOne(@Nonnull Bson filter) {
        return delegate.deleteOne(filter);
    }

//    @Nonnull
//    @Override
//    public DeleteResult deleteOne(@Nonnull Bson bson, @Nonnull DeleteOptions deleteOptions) {
//        return delegate.deleteOne(bson, deleteOptions);
//    }
//
//    @Nonnull
//    @Override
//    public DeleteResult deleteOne(@Nonnull ClientSession clientSession, @Nonnull Bson bson) {
//        return delegate.deleteOne(clientSession, bson);
//    }
//
//    @Nonnull
//    @Override
//    public DeleteResult deleteOne(@Nonnull ClientSession clientSession, @Nonnull Bson bson, @Nonnull DeleteOptions deleteOptions) {
//        return delegate.deleteOne(clientSession, bson, deleteOptions);
//    }

    @Nonnull
    @Override
    public DeleteResult deleteMany(@Nonnull Bson filter) {
        return delegate.deleteMany(filter);
    }

//    @Nonnull
//    @Override
//    public DeleteResult deleteMany(@Nonnull Bson bson, @Nonnull DeleteOptions deleteOptions) {
//        return delegate.deleteMany(bson, deleteOptions);
//    }
//
//    @Nonnull
//    @Override
//    public DeleteResult deleteMany(@Nonnull ClientSession clientSession, @Nonnull Bson bson) {
//        return delegate.deleteMany(clientSession, bson);
//    }
//
//    @Nonnull
//    @Override
//    public DeleteResult deleteMany(@Nonnull ClientSession clientSession, @Nonnull Bson bson, @Nonnull DeleteOptions deleteOptions) {
//        return delegate.deleteMany(clientSession, bson, deleteOptions);
//    }

    @Nonnull
    @Override
    public UpdateResult replaceOne(@Nonnull Bson filter, @Nonnull T entity) {
        return delegate.replaceOne(filter, entity);
    }

    @Nonnull
    @Override
    public UpdateResult replaceOne(@Nonnull Bson filter, @Nonnull T entity, @Nonnull ReplaceOptions options) {
        return delegate.replaceOne(filter, entity, options);
    }

//    @Nonnull
//    @Override
//    public UpdateResult replaceOne(@Nonnull ClientSession clientSession, @Nonnull Bson bson, @Nonnull T t) {
//        return delegate.replaceOne(clientSession, bson, t);
//    }
//
//    @Nonnull
//    @Override
//    public UpdateResult replaceOne(@Nonnull ClientSession clientSession, @Nonnull Bson bson, @Nonnull T t, @Nonnull ReplaceOptions replaceOptions) {
//        return delegate.replaceOne(clientSession, bson, t, replaceOptions);
//    }

    @Nonnull
    @Override
    public UpdateResult updateOne(@Nonnull Bson filter, @Nonnull Bson updates) {
        return delegate.updateOne(filter, updates);
    }

    @Nonnull
    @Override
    public UpdateResult updateOne(@Nonnull Bson filter, @Nonnull Bson updates, @Nonnull UpdateOptions options) {
        return delegate.updateOne(filter, updates, options);
    }


//    @Nonnull
//    @Override
//    public UpdateResult updateOne(@Nonnull ClientSession clientSession, @Nonnull Bson bson, @Nonnull Bson bson1) {
//        return delegate.updateOne(clientSession, bson, bson1);
//    }
//
//    @Nonnull
//    @Override
//    public UpdateResult updateOne(@Nonnull ClientSession clientSession, @Nonnull Bson bson, @Nonnull Bson bson1, @Nonnull UpdateOptions updateOptions) {
//        return delegate.updateOne(clientSession, bson, bson1, updateOptions);
//    }
//
//    @Nonnull
//    @Override
//    public UpdateResult updateOne(@Nonnull Bson bson, @Nonnull List<? extends Bson> list) {
//        return delegate.updateOne(bson, list);
//    }
//
//    @Nonnull
//    @Override
//    public UpdateResult updateOne(@Nonnull Bson bson, @Nonnull List<? extends Bson> list, @Nonnull UpdateOptions updateOptions) {
//        return delegate.updateOne(bson, list, updateOptions);
//    }
//
//    @Nonnull
//    @Override
//    public UpdateResult updateOne(@Nonnull ClientSession clientSession, @Nonnull Bson bson, @Nonnull List<? extends Bson> list) {
//        return delegate.updateOne(clientSession, bson, list);
//    }
//
//    @Nonnull
//    @Override
//    public UpdateResult updateOne(@Nonnull ClientSession clientSession, @Nonnull Bson bson, @Nonnull List<? extends Bson> list, @Nonnull UpdateOptions updateOptions) {
//        return delegate.updateOne(clientSession, bson, list, updateOptions);
//    }

    @Nonnull
    @Override
    public UpdateResult updateMany(@Nonnull Bson filter, @Nonnull Bson updates) {
        return delegate.updateMany(filter, updates);
    }

    @Nonnull
    @Override
    public UpdateResult updateMany(@Nonnull Bson filter, @Nonnull Bson updates, @Nonnull UpdateOptions options) {
        return delegate.updateMany(filter, updates, options);
    }

//    @Nonnull
//    @Override
//    public UpdateResult updateMany(@Nonnull ClientSession clientSession, @Nonnull Bson bson, @Nonnull Bson bson1) {
//        return delegate.updateMany(clientSession, bson, bson1);
//    }
//
//    @Nonnull
//    @Override
//    public UpdateResult updateMany(@Nonnull ClientSession clientSession, @Nonnull Bson bson, @Nonnull Bson bson1, @Nonnull UpdateOptions updateOptions) {
//        return delegate.updateMany(clientSession, bson, bson1, updateOptions);
//    }
//
//    @Nonnull
//    @Override
//    public UpdateResult updateMany(@Nonnull Bson bson, @Nonnull List<? extends Bson> list) {
//        return delegate.updateMany(bson, list);
//    }
//
//    @Nonnull
//    @Override
//    public UpdateResult updateMany(@Nonnull Bson bson, @Nonnull List<? extends Bson> list, @Nonnull UpdateOptions updateOptions) {
//        return delegate.updateMany(bson, list, updateOptions);
//    }
//
//    @Nonnull
//    @Override
//    public UpdateResult updateMany(@Nonnull ClientSession clientSession, @Nonnull Bson bson, @Nonnull List<? extends Bson> list) {
//        return delegate.updateMany(clientSession, bson, list);
//    }
//
//    @Nonnull
//    @Override
//    public UpdateResult updateMany(@Nonnull ClientSession clientSession, @Nonnull Bson bson, @Nonnull List<? extends Bson> list, @Nonnull UpdateOptions updateOptions) {
//        return delegate.updateMany(clientSession, bson, list, updateOptions);
//    }

    @Nullable
    @Override
    public T findOneAndDelete(@Nonnull Bson filter) {
        return delegate.findOneAndDelete(filter);
    }

//    @Nullable
//    @Override
//    public T findOneAndDelete(@Nonnull Bson bson, @Nonnull FindOneAndDeleteOptions findOneAndDeleteOptions) {
//        return delegate.findOneAndDelete(bson, findOneAndDeleteOptions);
//    }
//
//    @Nullable
//    @Override
//    public T findOneAndDelete(@Nonnull ClientSession clientSession, @Nonnull Bson bson) {
//        return delegate.findOneAndDelete(clientSession, bson);
//    }
//
//    @Nullable
//    @Override
//    public T findOneAndDelete(@Nonnull ClientSession clientSession, @Nonnull Bson bson, @Nonnull FindOneAndDeleteOptions findOneAndDeleteOptions) {
//        return delegate.findOneAndDelete(clientSession, bson, findOneAndDeleteOptions);
//    }
//
//    @Nullable
//    @Override
//    public T findOneAndReplace(@Nonnull Bson bson, @Nonnull T t) {
//        return delegate.findOneAndReplace(bson, t);
//    }

    @Nullable
    @Override
    public T findOneAndReplace(@Nonnull Bson filter, @Nonnull T entity, @Nonnull FindOneAndReplaceOptions options) {
        return delegate.findOneAndReplace(filter, entity, options);
    }


//    @Nullable
//    @Override
//    public T findOneAndReplace(@Nonnull ClientSession clientSession, @Nonnull Bson bson, @Nonnull T t) {
//        return delegate.findOneAndReplace(clientSession, bson, t);
//    }
//
//    @Nullable
//    @Override
//    public T findOneAndReplace(@Nonnull ClientSession clientSession, @Nonnull Bson bson, @Nonnull T t, @Nonnull FindOneAndReplaceOptions findOneAndReplaceOptions) {
//        return delegate.findOneAndReplace(clientSession, bson, t, findOneAndReplaceOptions);
//    }

    @Nullable
    @Override
    public T findOneAndUpdate(@Nonnull Bson filter, @Nonnull Bson updates) {
        return delegate.findOneAndUpdate(filter, updates);
    }

    @Nullable
    @Override
    public T findOneAndUpdate(@Nonnull Bson filter, @Nonnull Bson updates, @Nonnull FindOneAndUpdateOptions options) {
        return delegate.findOneAndUpdate(filter, updates, options);
    }

//    @Nullable
//    @Override
//    public T findOneAndUpdate(@Nonnull ClientSession clientSession, @Nonnull Bson bson, @Nonnull Bson bson1) {
//        return delegate.findOneAndUpdate(clientSession, bson, bson1);
//    }
//
//    @Nullable
//    @Override
//    public T findOneAndUpdate(@Nonnull ClientSession clientSession, @Nonnull Bson bson, @Nonnull Bson bson1, @Nonnull FindOneAndUpdateOptions findOneAndUpdateOptions) {
//        return delegate.findOneAndUpdate(clientSession, bson, bson1, findOneAndUpdateOptions);
//    }
//
//    @Nullable
//    @Override
//    public T findOneAndUpdate(@Nonnull Bson bson, @Nonnull List<? extends Bson> list) {
//        return delegate.findOneAndUpdate(bson, list);
//    }
//
//    @Nullable
//    @Override
//    public T findOneAndUpdate(@Nonnull Bson bson, @Nonnull List<? extends Bson> list, @Nonnull FindOneAndUpdateOptions findOneAndUpdateOptions) {
//        return delegate.findOneAndUpdate(bson, list, findOneAndUpdateOptions);
//    }
//
//    @Nullable
//    @Override
//    public T findOneAndUpdate(@Nonnull ClientSession clientSession, @Nonnull Bson bson, @Nonnull List<? extends Bson> list) {
//        return delegate.findOneAndUpdate(clientSession, bson, list);
//    }
//
//    @Nullable
//    @Override
//    public T findOneAndUpdate(@Nonnull ClientSession clientSession, @Nonnull Bson bson, @Nonnull List<? extends Bson> list, @Nonnull FindOneAndUpdateOptions findOneAndUpdateOptions) {
//        return delegate.findOneAndUpdate(clientSession, bson, list, findOneAndUpdateOptions);
//    }

    @Override
    public void drop() {
        delegate.drop();
    }

    //    @Override
//    public void drop(@Nonnull ClientSession clientSession) {
//        delegate.drop(clientSession);
//    }
//
//    @Override
//    public void drop(@Nonnull DropCollectionOptions dropCollectionOptions) {
//        delegate.drop(dropCollectionOptions);
//    }
//
//    @Override
//    public void drop(@Nonnull ClientSession clientSession, @Nonnull DropCollectionOptions dropCollectionOptions) {
//        delegate.drop(clientSession, dropCollectionOptions);
//    }
//
//    @Nonnull
//    @Override
//    public String createSearchIndex(@Nonnull String s, @Nonnull Bson bson) {
//        return delegate.createSearchIndex(s, bson);
//    }
//
//    @Nonnull
//    @Override
//    public String createSearchIndex(@Nonnull Bson bson) {
//        return delegate.createSearchIndex(bson);
//    }
//
//    @Nonnull
//    @Override
//    public List<String> createSearchIndexes(@Nonnull List<SearchIndexModel> list) {
//        return delegate.createSearchIndexes(list);
//    }
//
//    @Override
//    public void updateSearchIndex(@Nonnull String s, @Nonnull Bson bson) {
//        delegate.updateSearchIndex(s, bson);
//    }
//
//    @Override
//    public void dropSearchIndex(@Nonnull String s) {
//        delegate.dropSearchIndex(s);
//    }
//
//    @Nonnull
//    @Override
//    public ListSearchIndexesIterable<Document> listSearchIndexes() {
//        return delegate.listSearchIndexes();
//    }
//
//    @Nonnull
//    @Override
//    public <TResult> ListSearchIndexesIterable<TResult> listSearchIndexes(@Nonnull Class<TResult> aClass) {
//        return delegate.listSearchIndexes(aClass);
//    }

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

//    @Nonnull
//    @Override
//    public String createIndex(@Nonnull ClientSession clientSession, @Nonnull Bson bson) {
//        return delegate.createIndex(clientSession, bson);
//    }
//
//    @Nonnull
//    @Override
//    public String createIndex(@Nonnull ClientSession clientSession, @Nonnull Bson bson, @Nonnull IndexOptions indexOptions) {
//        return delegate.createIndex(clientSession, bson, indexOptions);
//    }

    @Nonnull
    @Override
    public List<String> createIndexes(@Nonnull List<IndexModel> indexes) {
        return delegate.createIndexes(indexes);
    }

//    @Nonnull
//    @Override
//    public List<String> createIndexes(@Nonnull List<IndexModel> list, @Nonnull CreateIndexOptions createIndexOptions) {
//        return delegate.createIndexes(list, createIndexOptions);
//    }
//
//    @Nonnull
//    @Override
//    public List<String> createIndexes(@Nonnull ClientSession clientSession, @Nonnull List<IndexModel> list) {
//        return delegate.createIndexes(clientSession, list);
//    }
//
//    @Nonnull
//    @Override
//    public List<String> createIndexes(@Nonnull ClientSession clientSession, @Nonnull List<IndexModel> list, @Nonnull CreateIndexOptions createIndexOptions) {
//        return delegate.createIndexes(clientSession, list, createIndexOptions);
//    }

    @Nonnull
    @Override
    public ListIndexesIterable<Document> listIndexes() {
        return delegate.listIndexes();
    }

//    @Nonnull
//    @Override
//    public <TResult> ListIndexesIterable<TResult> listIndexes(@Nonnull Class<TResult> aClass) {
//        return delegate.listIndexes(aClass);
//    }
//
//    @Nonnull
//    @Override
//    public ListIndexesIterable<Document> listIndexes(@Nonnull ClientSession clientSession) {
//        return delegate.listIndexes(clientSession);
//    }
//
//    @Nonnull
//    @Override
//    public <TResult> ListIndexesIterable<TResult> listIndexes(@Nonnull ClientSession clientSession, @Nonnull Class<TResult> aClass) {
//        return delegate.listIndexes(clientSession, aClass);
//    }

    @Override
    public void dropIndex(@Nonnull String indexName) {
        delegate.dropIndex(indexName);
    }

//    @Override
//    public void dropIndex(@Nonnull String s, @Nonnull DropIndexOptions dropIndexOptions) {
//        delegate.dropIndex(s, dropIndexOptions);
//    }
//
//    @Override
//    public void dropIndex(@Nonnull Bson bson) {
//        delegate.dropIndex(bson);
//    }
//
//    @Override
//    public void dropIndex(@Nonnull Bson bson, @Nonnull DropIndexOptions dropIndexOptions) {
//        delegate.dropIndex(bson, dropIndexOptions);
//    }
//
//    @Override
//    public void dropIndex(@Nonnull ClientSession clientSession, @Nonnull String s) {
//        delegate.dropIndex(clientSession, s);
//    }
//
//    @Override
//    public void dropIndex(@Nonnull ClientSession clientSession, @Nonnull Bson bson) {
//        delegate.dropIndex(clientSession, bson);
//    }
//
//    @Override
//    public void dropIndex(@Nonnull ClientSession clientSession, @Nonnull String s, @Nonnull DropIndexOptions dropIndexOptions) {
//        delegate.dropIndex(clientSession, s, dropIndexOptions);
//    }
//
//    @Override
//    public void dropIndex(@Nonnull ClientSession clientSession, @Nonnull Bson bson, @Nonnull DropIndexOptions dropIndexOptions) {
//        delegate.dropIndex(clientSession, bson, dropIndexOptions);
//    }

    @Override
    public void dropIndexes() {
        delegate.dropIndexes();
    }

//    @Override
//    public void dropIndexes(@Nonnull ClientSession clientSession) {
//        delegate.dropIndexes(clientSession);
//    }
//
//    @Override
//    public void dropIndexes(@Nonnull DropIndexOptions dropIndexOptions) {
//        delegate.dropIndexes(dropIndexOptions);
//    }
//
//    @Override
//    public void dropIndexes(@Nonnull ClientSession clientSession, @Nonnull DropIndexOptions dropIndexOptions) {
//        delegate.dropIndexes(clientSession, dropIndexOptions);
//    }
//
//    @Override
//    public void renameCollection(@Nonnull MongoNamespace mongoNamespace) {
//        delegate.renameCollection(mongoNamespace);
//    }
//
//    @Override
//    public void renameCollection(@Nonnull MongoNamespace mongoNamespace, @Nonnull RenameCollectionOptions renameCollectionOptions) {
//        delegate.renameCollection(mongoNamespace, renameCollectionOptions);
//    }
//
//    @Override
//    public void renameCollection(@Nonnull ClientSession clientSession, @Nonnull MongoNamespace mongoNamespace) {
//        delegate.renameCollection(clientSession, mongoNamespace);
//    }
//
//    @Override
//    public void renameCollection(@Nonnull ClientSession clientSession, @Nonnull MongoNamespace mongoNamespace, @Nonnull RenameCollectionOptions renameCollectionOptions) {
//        delegate.renameCollection(clientSession, mongoNamespace, renameCollectionOptions);
//    }
}
