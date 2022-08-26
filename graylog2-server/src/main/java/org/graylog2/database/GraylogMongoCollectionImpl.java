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
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.ClientSession;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MapReduceIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.CreateIndexOptions;
import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.model.DropCollectionOptions;
import com.mongodb.client.model.DropIndexOptions;
import com.mongodb.client.model.EstimatedDocumentCountOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndDeleteOptions;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.InsertOneOptions;
import com.mongodb.client.model.RenameCollectionOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.lang.Nullable;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.graylog.shaded.mongojack4.org.mongojack.JacksonMongoCollection;

import java.util.List;

public class GraylogMongoCollectionImpl<TDocument> implements GraylogMongoCollection<TDocument> {

    private final JacksonMongoCollection<TDocument> delegate;

    public GraylogMongoCollectionImpl(JacksonMongoCollection<TDocument> delegate) {
        this.delegate = delegate;
    }

    // TODO: this implementation is inefficient and probably not safe in every scenario. Consider rewriting or removing
    //   it. It's just here to make migration easier
    @Override
    public TDocument save(TDocument dto) {
        final BsonValue upsertedId = delegate.save(dto).getUpsertedId();
        if (upsertedId == null) {
            return dto;
        } else {
            return delegate.find(Filters.eq(upsertedId)).first();
        }
    }

    // ------------------
    // starting from here, all methods are delegating to the underlying jackson mongo collection
    // ------------------

    @Override
    public MongoNamespace getNamespace() {
        return delegate.getNamespace();
    }

    @Override
    public Class<TDocument> getDocumentClass() {
        return delegate.getDocumentClass();
    }

    @Override
    public CodecRegistry getCodecRegistry() {
        return delegate.getCodecRegistry();
    }

    @Override
    public ReadPreference getReadPreference() {
        return delegate.getReadPreference();
    }

    @Override
    public WriteConcern getWriteConcern() {
        return delegate.getWriteConcern();
    }

    @Override
    public ReadConcern getReadConcern() {
        return delegate.getReadConcern();
    }

    @Override
    public <NewTDocument> MongoCollection<NewTDocument> withDocumentClass(Class<NewTDocument> clazz) {
        return delegate.withDocumentClass(clazz);
    }

    @Override
    public MongoCollection<TDocument> withCodecRegistry(CodecRegistry codecRegistry) {
        return delegate.withCodecRegistry(codecRegistry);
    }

    @Override
    public MongoCollection<TDocument> withReadPreference(ReadPreference readPreference) {
        return delegate.withReadPreference(readPreference);
    }

    @Override
    public MongoCollection<TDocument> withWriteConcern(WriteConcern writeConcern) {
        return delegate.withWriteConcern(writeConcern);
    }

    @Override
    public MongoCollection<TDocument> withReadConcern(ReadConcern readConcern) {
        return delegate.withReadConcern(readConcern);
    }

    @Override
    public long countDocuments() {
        return delegate.countDocuments();
    }

    @Override
    public long countDocuments(Bson filter) {
        return delegate.countDocuments(filter);
    }

    @Override
    public long countDocuments(Bson filter, CountOptions options) {
        return delegate.countDocuments(filter, options);
    }

    @Override
    public long countDocuments(ClientSession clientSession) {
        return delegate.countDocuments(clientSession);
    }

    @Override
    public long countDocuments(ClientSession clientSession, Bson filter) {
        return delegate.countDocuments(clientSession, filter);
    }

    @Override
    public long countDocuments(ClientSession clientSession, Bson filter, CountOptions options) {
        return delegate.countDocuments(clientSession, filter, options);
    }

    @Override
    public long estimatedDocumentCount() {
        return delegate.estimatedDocumentCount();
    }

    @Override
    public long estimatedDocumentCount(EstimatedDocumentCountOptions options) {
        return delegate.estimatedDocumentCount(options);
    }

    @Override
    public <TResult> DistinctIterable<TResult> distinct(String fieldName, Class<TResult> tResultClass) {
        return delegate.distinct(fieldName, tResultClass);
    }

    @Override
    public <TResult> DistinctIterable<TResult> distinct(String fieldName, Bson filter, Class<TResult> tResultClass) {
        return delegate.distinct(fieldName, filter, tResultClass);
    }

    @Override
    public <TResult> DistinctIterable<TResult> distinct(ClientSession clientSession, String fieldName, Class<TResult> tResultClass) {
        return delegate.distinct(clientSession, fieldName, tResultClass);
    }

    @Override
    public <TResult> DistinctIterable<TResult> distinct(ClientSession clientSession, String fieldName, Bson filter, Class<TResult> tResultClass) {
        return delegate.distinct(clientSession, fieldName, filter, tResultClass);
    }

    @Override
    public FindIterable<TDocument> find() {
        return delegate.find();
    }

    @Override
    public <TResult> FindIterable<TResult> find(Class<TResult> tResultClass) {
        return delegate.find(tResultClass);
    }

    @Override
    public FindIterable<TDocument> find(Bson filter) {
        return delegate.find(filter);
    }

    @Override
    public <TResult> FindIterable<TResult> find(Bson filter, Class<TResult> tResultClass) {
        return delegate.find(filter, tResultClass);
    }

    @Override
    public FindIterable<TDocument> find(ClientSession clientSession) {
        return delegate.find(clientSession);
    }

    @Override
    public <TResult> FindIterable<TResult> find(ClientSession clientSession, Class<TResult> tResultClass) {
        return delegate.find(clientSession, tResultClass);
    }

    @Override
    public FindIterable<TDocument> find(ClientSession clientSession, Bson filter) {
        return delegate.find(clientSession, filter);
    }

    @Override
    public <TResult> FindIterable<TResult> find(ClientSession clientSession, Bson filter, Class<TResult> tResultClass) {
        return delegate.find(clientSession, filter, tResultClass);
    }

    @Override
    public AggregateIterable<TDocument> aggregate(List<? extends Bson> pipeline) {
        return delegate.aggregate(pipeline);
    }

    @Override
    public <TResult> AggregateIterable<TResult> aggregate(List<? extends Bson> pipeline, Class<TResult> tResultClass) {
        return delegate.aggregate(pipeline, tResultClass);
    }

    @Override
    public AggregateIterable<TDocument> aggregate(ClientSession clientSession, List<? extends Bson> pipeline) {
        return delegate.aggregate(clientSession, pipeline);
    }

    @Override
    public <TResult> AggregateIterable<TResult> aggregate(ClientSession clientSession, List<? extends Bson> pipeline, Class<TResult> tResultClass) {
        return delegate.aggregate(clientSession, pipeline, tResultClass);
    }

    @Override
    public ChangeStreamIterable<TDocument> watch() {
        return delegate.watch();
    }

    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(Class<TResult> tResultClass) {
        return delegate.watch(tResultClass);
    }

    @Override
    public ChangeStreamIterable<TDocument> watch(List<? extends Bson> pipeline) {
        return delegate.watch(pipeline);
    }

    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(List<? extends Bson> pipeline, Class<TResult> tResultClass) {
        return delegate.watch(pipeline, tResultClass);
    }

    @Override
    public ChangeStreamIterable<TDocument> watch(ClientSession clientSession) {
        return delegate.watch(clientSession);
    }

    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(ClientSession clientSession, Class<TResult> tResultClass) {
        return delegate.watch(clientSession, tResultClass);
    }

    @Override
    public ChangeStreamIterable<TDocument> watch(ClientSession clientSession, List<? extends Bson> pipeline) {
        return delegate.watch(clientSession, pipeline);
    }

    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(ClientSession clientSession, List<? extends Bson> pipeline, Class<TResult> tResultClass) {
        return delegate.watch(clientSession, pipeline, tResultClass);
    }

    @Override
    @Deprecated
    public MapReduceIterable<TDocument> mapReduce(String mapFunction, String reduceFunction) {
        return delegate.mapReduce(mapFunction, reduceFunction);
    }

    @Override
    @Deprecated
    public <TResult> MapReduceIterable<TResult> mapReduce(String mapFunction, String reduceFunction, Class<TResult> tResultClass) {
        return delegate.mapReduce(mapFunction, reduceFunction, tResultClass);
    }

    @Override
    @Deprecated
    public MapReduceIterable<TDocument> mapReduce(ClientSession clientSession, String mapFunction, String reduceFunction) {
        return delegate.mapReduce(clientSession, mapFunction, reduceFunction);
    }

    @Override
    @Deprecated
    public <TResult> MapReduceIterable<TResult> mapReduce(ClientSession clientSession, String mapFunction, String reduceFunction, Class<TResult> tResultClass) {
        return delegate.mapReduce(clientSession, mapFunction, reduceFunction, tResultClass);
    }

    @Override
    public BulkWriteResult bulkWrite(List<? extends WriteModel<? extends TDocument>> requests) {
        return delegate.bulkWrite(requests);
    }

    @Override
    public BulkWriteResult bulkWrite(List<? extends WriteModel<? extends TDocument>> requests, BulkWriteOptions options) {
        return delegate.bulkWrite(requests, options);
    }

    @Override
    public BulkWriteResult bulkWrite(ClientSession clientSession, List<? extends WriteModel<? extends TDocument>> requests) {
        return delegate.bulkWrite(clientSession, requests);
    }

    @Override
    public BulkWriteResult bulkWrite(ClientSession clientSession, List<? extends WriteModel<? extends TDocument>> requests, BulkWriteOptions options) {
        return delegate.bulkWrite(clientSession, requests, options);
    }

    @Override
    public InsertOneResult insertOne(TDocument tDocument) {
        return delegate.insertOne(tDocument);
    }

    @Override
    public InsertOneResult insertOne(TDocument tDocument, InsertOneOptions options) {
        return delegate.insertOne(tDocument, options);
    }

    @Override
    public InsertOneResult insertOne(ClientSession clientSession, TDocument tDocument) {
        return delegate.insertOne(clientSession, tDocument);
    }

    @Override
    public InsertOneResult insertOne(ClientSession clientSession, TDocument tDocument, InsertOneOptions options) {
        return delegate.insertOne(clientSession, tDocument, options);
    }

    @Override
    public InsertManyResult insertMany(List<? extends TDocument> tDocuments) {
        return delegate.insertMany(tDocuments);
    }

    @Override
    public InsertManyResult insertMany(List<? extends TDocument> tDocuments, InsertManyOptions options) {
        return delegate.insertMany(tDocuments, options);
    }

    @Override
    public InsertManyResult insertMany(ClientSession clientSession, List<? extends TDocument> tDocuments) {
        return delegate.insertMany(clientSession, tDocuments);
    }

    @Override
    public InsertManyResult insertMany(ClientSession clientSession, List<? extends TDocument> tDocuments, InsertManyOptions options) {
        return delegate.insertMany(clientSession, tDocuments, options);
    }

    @Override
    public DeleteResult deleteOne(Bson filter) {
        return delegate.deleteOne(filter);
    }

    @Override
    public DeleteResult deleteOne(Bson filter, DeleteOptions options) {
        return delegate.deleteOne(filter, options);
    }

    @Override
    public DeleteResult deleteOne(ClientSession clientSession, Bson filter) {
        return delegate.deleteOne(clientSession, filter);
    }

    @Override
    public DeleteResult deleteOne(ClientSession clientSession, Bson filter, DeleteOptions options) {
        return delegate.deleteOne(clientSession, filter, options);
    }

    @Override
    public DeleteResult deleteMany(Bson filter) {
        return delegate.deleteMany(filter);
    }

    @Override
    public DeleteResult deleteMany(Bson filter, DeleteOptions options) {
        return delegate.deleteMany(filter, options);
    }

    @Override
    public DeleteResult deleteMany(ClientSession clientSession, Bson filter) {
        return delegate.deleteMany(clientSession, filter);
    }

    @Override
    public DeleteResult deleteMany(ClientSession clientSession, Bson filter, DeleteOptions options) {
        return delegate.deleteMany(clientSession, filter, options);
    }

    @Override
    public UpdateResult replaceOne(Bson filter, TDocument replacement) {
        return delegate.replaceOne(filter, replacement);
    }

    @Override
    public UpdateResult replaceOne(Bson filter, TDocument replacement, ReplaceOptions replaceOptions) {
        return delegate.replaceOne(filter, replacement, replaceOptions);
    }

    @Override
    public UpdateResult replaceOne(ClientSession clientSession, Bson filter, TDocument replacement) {
        return delegate.replaceOne(clientSession, filter, replacement);
    }

    @Override
    public UpdateResult replaceOne(ClientSession clientSession, Bson filter, TDocument replacement, ReplaceOptions replaceOptions) {
        return delegate.replaceOne(clientSession, filter, replacement, replaceOptions);
    }

    @Override
    public UpdateResult updateOne(Bson filter, Bson update) {
        return delegate.updateOne(filter, update);
    }

    @Override
    public UpdateResult updateOne(Bson filter, Bson update, UpdateOptions updateOptions) {
        return delegate.updateOne(filter, update, updateOptions);
    }

    @Override
    public UpdateResult updateOne(ClientSession clientSession, Bson filter, Bson update) {
        return delegate.updateOne(clientSession, filter, update);
    }

    @Override
    public UpdateResult updateOne(ClientSession clientSession, Bson filter, Bson update, UpdateOptions updateOptions) {
        return delegate.updateOne(clientSession, filter, update, updateOptions);
    }

    @Override
    public UpdateResult updateOne(Bson filter, List<? extends Bson> update) {
        return delegate.updateOne(filter, update);
    }

    @Override
    public UpdateResult updateOne(Bson filter, List<? extends Bson> update, UpdateOptions updateOptions) {
        return delegate.updateOne(filter, update, updateOptions);
    }

    @Override
    public UpdateResult updateOne(ClientSession clientSession, Bson filter, List<? extends Bson> update) {
        return delegate.updateOne(clientSession, filter, update);
    }

    @Override
    public UpdateResult updateOne(ClientSession clientSession, Bson filter, List<? extends Bson> update, UpdateOptions updateOptions) {
        return delegate.updateOne(clientSession, filter, update, updateOptions);
    }

    @Override
    public UpdateResult updateMany(Bson filter, Bson update) {
        return delegate.updateMany(filter, update);
    }

    @Override
    public UpdateResult updateMany(Bson filter, Bson update, UpdateOptions updateOptions) {
        return delegate.updateMany(filter, update, updateOptions);
    }

    @Override
    public UpdateResult updateMany(ClientSession clientSession, Bson filter, Bson update) {
        return delegate.updateMany(clientSession, filter, update);
    }

    @Override
    public UpdateResult updateMany(ClientSession clientSession, Bson filter, Bson update, UpdateOptions updateOptions) {
        return delegate.updateMany(clientSession, filter, update, updateOptions);
    }

    @Override
    public UpdateResult updateMany(Bson filter, List<? extends Bson> update) {
        return delegate.updateMany(filter, update);
    }

    @Override
    public UpdateResult updateMany(Bson filter, List<? extends Bson> update, UpdateOptions updateOptions) {
        return delegate.updateMany(filter, update, updateOptions);
    }

    @Override
    public UpdateResult updateMany(ClientSession clientSession, Bson filter, List<? extends Bson> update) {
        return delegate.updateMany(clientSession, filter, update);
    }

    @Override
    public UpdateResult updateMany(ClientSession clientSession, Bson filter, List<? extends Bson> update, UpdateOptions updateOptions) {
        return delegate.updateMany(clientSession, filter, update, updateOptions);
    }

    @Override
    @Nullable
    public TDocument findOneAndDelete(Bson filter) {
        return delegate.findOneAndDelete(filter);
    }

    @Override
    @Nullable
    public TDocument findOneAndDelete(Bson filter, FindOneAndDeleteOptions options) {
        return delegate.findOneAndDelete(filter, options);
    }

    @Override
    @Nullable
    public TDocument findOneAndDelete(ClientSession clientSession, Bson filter) {
        return delegate.findOneAndDelete(clientSession, filter);
    }

    @Override
    @Nullable
    public TDocument findOneAndDelete(ClientSession clientSession, Bson filter, FindOneAndDeleteOptions options) {
        return delegate.findOneAndDelete(clientSession, filter, options);
    }

    @Override
    @Nullable
    public TDocument findOneAndReplace(Bson filter, TDocument replacement) {
        return delegate.findOneAndReplace(filter, replacement);
    }

    @Override
    @Nullable
    public TDocument findOneAndReplace(Bson filter, TDocument replacement, FindOneAndReplaceOptions options) {
        return delegate.findOneAndReplace(filter, replacement, options);
    }

    @Override
    @Nullable
    public TDocument findOneAndReplace(ClientSession clientSession, Bson filter, TDocument replacement) {
        return delegate.findOneAndReplace(clientSession, filter, replacement);
    }

    @Override
    @Nullable
    public TDocument findOneAndReplace(ClientSession clientSession, Bson filter, TDocument replacement, FindOneAndReplaceOptions options) {
        return delegate.findOneAndReplace(clientSession, filter, replacement, options);
    }

    @Override
    @Nullable
    public TDocument findOneAndUpdate(Bson filter, Bson update) {
        return delegate.findOneAndUpdate(filter, update);
    }

    @Override
    @Nullable
    public TDocument findOneAndUpdate(Bson filter, Bson update, FindOneAndUpdateOptions options) {
        return delegate.findOneAndUpdate(filter, update, options);
    }

    @Override
    @Nullable
    public TDocument findOneAndUpdate(ClientSession clientSession, Bson filter, Bson update) {
        return delegate.findOneAndUpdate(clientSession, filter, update);
    }

    @Override
    @Nullable
    public TDocument findOneAndUpdate(ClientSession clientSession, Bson filter, Bson update, FindOneAndUpdateOptions options) {
        return delegate.findOneAndUpdate(clientSession, filter, update, options);
    }

    @Override
    @Nullable
    public TDocument findOneAndUpdate(Bson filter, List<? extends Bson> update) {
        return delegate.findOneAndUpdate(filter, update);
    }

    @Override
    @Nullable
    public TDocument findOneAndUpdate(Bson filter, List<? extends Bson> update, FindOneAndUpdateOptions options) {
        return delegate.findOneAndUpdate(filter, update, options);
    }

    @Override
    @Nullable
    public TDocument findOneAndUpdate(ClientSession clientSession, Bson filter, List<? extends Bson> update) {
        return delegate.findOneAndUpdate(clientSession, filter, update);
    }

    @Override
    @Nullable
    public TDocument findOneAndUpdate(ClientSession clientSession, Bson filter, List<? extends Bson> update, FindOneAndUpdateOptions options) {
        return delegate.findOneAndUpdate(clientSession, filter, update, options);
    }

    @Override
    public void drop() {
        delegate.drop();
    }

    @Override
    public void drop(ClientSession clientSession) {
        delegate.drop(clientSession);
    }

    @Override
    public void drop(DropCollectionOptions dropCollectionOptions) {
        delegate.drop(dropCollectionOptions);
    }

    @Override
    public void drop(ClientSession clientSession, DropCollectionOptions dropCollectionOptions) {
        delegate.drop(clientSession, dropCollectionOptions);
    }

    @Override
    public String createIndex(Bson keys) {
        return delegate.createIndex(keys);
    }

    @Override
    public String createIndex(Bson keys, IndexOptions indexOptions) {
        return delegate.createIndex(keys, indexOptions);
    }

    @Override
    public String createIndex(ClientSession clientSession, Bson keys) {
        return delegate.createIndex(clientSession, keys);
    }

    @Override
    public String createIndex(ClientSession clientSession, Bson keys, IndexOptions indexOptions) {
        return delegate.createIndex(clientSession, keys, indexOptions);
    }

    @Override
    public List<String> createIndexes(List<IndexModel> indexes) {
        return delegate.createIndexes(indexes);
    }

    @Override
    public List<String> createIndexes(List<IndexModel> indexes, CreateIndexOptions createIndexOptions) {
        return delegate.createIndexes(indexes, createIndexOptions);
    }

    @Override
    public List<String> createIndexes(ClientSession clientSession, List<IndexModel> indexes) {
        return delegate.createIndexes(clientSession, indexes);
    }

    @Override
    public List<String> createIndexes(ClientSession clientSession, List<IndexModel> indexes, CreateIndexOptions createIndexOptions) {
        return delegate.createIndexes(clientSession, indexes, createIndexOptions);
    }

    @Override
    public ListIndexesIterable<Document> listIndexes() {
        return delegate.listIndexes();
    }

    @Override
    public <TResult> ListIndexesIterable<TResult> listIndexes(Class<TResult> tResultClass) {
        return delegate.listIndexes(tResultClass);
    }

    @Override
    public ListIndexesIterable<Document> listIndexes(ClientSession clientSession) {
        return delegate.listIndexes(clientSession);
    }

    @Override
    public <TResult> ListIndexesIterable<TResult> listIndexes(ClientSession clientSession, Class<TResult> tResultClass) {
        return delegate.listIndexes(clientSession, tResultClass);
    }

    @Override
    public void dropIndex(String indexName) {
        delegate.dropIndex(indexName);
    }

    @Override
    public void dropIndex(String indexName, DropIndexOptions dropIndexOptions) {
        delegate.dropIndex(indexName, dropIndexOptions);
    }

    @Override
    public void dropIndex(Bson keys) {
        delegate.dropIndex(keys);
    }

    @Override
    public void dropIndex(Bson keys, DropIndexOptions dropIndexOptions) {
        delegate.dropIndex(keys, dropIndexOptions);
    }

    @Override
    public void dropIndex(ClientSession clientSession, String indexName) {
        delegate.dropIndex(clientSession, indexName);
    }

    @Override
    public void dropIndex(ClientSession clientSession, Bson keys) {
        delegate.dropIndex(clientSession, keys);
    }

    @Override
    public void dropIndex(ClientSession clientSession, String indexName, DropIndexOptions dropIndexOptions) {
        delegate.dropIndex(clientSession, indexName, dropIndexOptions);
    }

    @Override
    public void dropIndex(ClientSession clientSession, Bson keys, DropIndexOptions dropIndexOptions) {
        delegate.dropIndex(clientSession, keys, dropIndexOptions);
    }

    @Override
    public void dropIndexes() {
        delegate.dropIndexes();
    }

    @Override
    public void dropIndexes(ClientSession clientSession) {
        delegate.dropIndexes(clientSession);
    }

    @Override
    public void dropIndexes(DropIndexOptions dropIndexOptions) {
        delegate.dropIndexes(dropIndexOptions);
    }

    @Override
    public void dropIndexes(ClientSession clientSession, DropIndexOptions dropIndexOptions) {
        delegate.dropIndexes(clientSession, dropIndexOptions);
    }

    @Override
    public void renameCollection(MongoNamespace newCollectionNamespace) {
        delegate.renameCollection(newCollectionNamespace);
    }

    @Override
    public void renameCollection(MongoNamespace newCollectionNamespace, RenameCollectionOptions renameCollectionOptions) {
        delegate.renameCollection(newCollectionNamespace, renameCollectionOptions);
    }

    @Override
    public void renameCollection(ClientSession clientSession, MongoNamespace newCollectionNamespace) {
        delegate.renameCollection(clientSession, newCollectionNamespace);
    }

    @Override
    public void renameCollection(ClientSession clientSession, MongoNamespace newCollectionNamespace, RenameCollectionOptions renameCollectionOptions) {
        delegate.renameCollection(clientSession, newCollectionNamespace, renameCollectionOptions);
    }
}
