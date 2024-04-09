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

import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoException;
import com.mongodb.MongoNamespace;
import com.mongodb.MongoServerException;
import com.mongodb.MongoWriteConcernException;
import com.mongodb.MongoWriteException;
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
import com.mongodb.client.ListSearchIndexesIterable;
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
import com.mongodb.client.model.SearchIndexModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.lang.Nullable;
import org.bson.Document;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Optional;

public class DefaultGraylogMongoCollection<T> implements GraylogMongoCollection<T> {
    private final MongoCollection<T> delegate;

    public DefaultGraylogMongoCollection(MongoCollection<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public PaginatedCollection<T> findPaginated() {
        return new PaginationDecorator<T>(this);
    }

    @Override
    public Optional<T> getById(ObjectId id) {
        return Optional.ofNullable(delegate.find(Filters.eq("_id", id)).first());
    }

    @Override
    public boolean deleteById(ObjectId id) {
        return delegate.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
    }

    /* ==== Only delegated methods below this line ==== */

    /**
     * Gets the namespace of this collection.
     *
     * @return the namespace
     */
    @Override
    public MongoNamespace getNamespace() {
        return delegate.getNamespace();
    }

    /**
     * Get the class of documents stored in this collection.
     *
     * @return the class
     */
    @Override
    public Class<T> getDocumentClass() {
        return delegate.getDocumentClass();
    }

    /**
     * Get the codec registry for the MongoCollection.
     *
     * @return the {@link CodecRegistry}
     */
    @Override
    public CodecRegistry getCodecRegistry() {
        return delegate.getCodecRegistry();
    }

    /**
     * Get the read preference for the MongoCollection.
     *
     * @return the {@link ReadPreference}
     */
    @Override
    public ReadPreference getReadPreference() {
        return delegate.getReadPreference();
    }

    /**
     * Get the write concern for the MongoCollection.
     *
     * @return the {@link WriteConcern}
     */
    @Override
    public WriteConcern getWriteConcern() {
        return delegate.getWriteConcern();
    }

    /**
     * Get the read concern for the MongoCollection.
     *
     * @return the {@link ReadConcern}
     * @mongodb.server.release 3.2
     * @mongodb.driver.manual reference/readConcern/ Read Concern
     * @since 3.2
     */
    @Override
    public ReadConcern getReadConcern() {
        return delegate.getReadConcern();
    }

    /**
     * Create a new MongoCollection instance with a different default class to cast any documents returned from the database into..
     *
     * @param clazz the default class to cast any documents returned from the database into.
     * @return a new MongoCollection instance with the different default class
     */
    @Override
    public <NewTDocument> MongoCollection<NewTDocument> withDocumentClass(Class<NewTDocument> clazz) {
        return delegate.withDocumentClass(clazz);
    }

    /**
     * Create a new MongoCollection instance with a different codec registry.
     *
     * <p>The {@link CodecRegistry} configured by this method is effectively treated by the driver as an instance of
     * {@link CodecProvider}, which {@link CodecRegistry} extends. So there is no benefit to defining
     * a class that implements {@link CodecRegistry}. Rather, an application should always create {@link CodecRegistry} instances
     * using the factory methods in {@link CodecRegistries}.</p>
     *
     * @param codecRegistry the new {@link CodecRegistry} for the collection
     * @return a new MongoCollection instance with the different codec registry
     * @see CodecRegistries
     */
    @Override
    public MongoCollection<T> withCodecRegistry(CodecRegistry codecRegistry) {
        return delegate.withCodecRegistry(codecRegistry);
    }

    /**
     * Create a new MongoCollection instance with a different read preference.
     *
     * @param readPreference the new {@link ReadPreference} for the collection
     * @return a new MongoCollection instance with the different readPreference
     */
    @Override
    public MongoCollection<T> withReadPreference(ReadPreference readPreference) {
        return delegate.withReadPreference(readPreference);
    }

    /**
     * Create a new MongoCollection instance with a different write concern.
     *
     * @param writeConcern the new {@link WriteConcern} for the collection
     * @return a new MongoCollection instance with the different writeConcern
     */
    @Override
    public MongoCollection<T> withWriteConcern(WriteConcern writeConcern) {
        return delegate.withWriteConcern(writeConcern);
    }

    /**
     * Create a new MongoCollection instance with a different read concern.
     *
     * @param readConcern the new {@link ReadConcern} for the collection
     * @return a new MongoCollection instance with the different ReadConcern
     * @mongodb.server.release 3.2
     * @mongodb.driver.manual reference/readConcern/ Read Concern
     * @since 3.2
     */
    @Override
    public MongoCollection<T> withReadConcern(ReadConcern readConcern) {
        return delegate.withReadConcern(readConcern);
    }

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
     * @since 3.8
     */
    @Override
    public long countDocuments() {
        return delegate.countDocuments();
    }

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
    @Override
    public long countDocuments(Bson filter) {
        return delegate.countDocuments(filter);
    }

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
     * @param filter  the query filter
     * @param options the options describing the count
     * @return the number of documents in the collection
     * @since 3.8
     */
    @Override
    public long countDocuments(Bson filter, CountOptions options) {
        return delegate.countDocuments(filter, options);
    }

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
     * @param clientSession the client session with which to associate this operation
     * @return the number of documents in the collection
     * @mongodb.server.release 3.6
     * @since 3.8
     */
    @Override
    public long countDocuments(ClientSession clientSession) {
        return delegate.countDocuments(clientSession);
    }

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
     * @param clientSession the client session with which to associate this operation
     * @param filter        the query filter
     * @return the number of documents in the collection
     * @mongodb.server.release 3.6
     * @since 3.8
     */
    @Override
    public long countDocuments(ClientSession clientSession, Bson filter) {
        return delegate.countDocuments(clientSession, filter);
    }

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
     * @param clientSession the client session with which to associate this operation
     * @param filter        the query filter
     * @param options       the options describing the count
     * @return the number of documents in the collection
     * @mongodb.server.release 3.6
     * @since 3.8
     */
    @Override
    public long countDocuments(ClientSession clientSession, Bson filter, CountOptions options) {
        return delegate.countDocuments(clientSession, filter, options);
    }

    /**
     * Gets an estimate of the count of documents in a collection using collection metadata.
     *
     * <p>
     * Implementation note: this method is implemented using the MongoDB server's count command
     * </p>
     *
     * @return the number of documents in the collection
     * @mongodb.driver.manual manual/reference/command/count/#behavior
     * @since 3.8
     */
    @Override
    public long estimatedDocumentCount() {
        return delegate.estimatedDocumentCount();
    }

    /**
     * Gets an estimate of the count of documents in a collection using collection metadata.
     *
     * <p>
     * Implementation note: this method is implemented using the MongoDB server's count command
     * </p>
     *
     * @param options the options describing the count
     * @return the number of documents in the collection
     * @mongodb.driver.manual manual/reference/command/count/#behavior
     * @since 3.8
     */
    @Override
    public long estimatedDocumentCount(EstimatedDocumentCountOptions options) {
        return delegate.estimatedDocumentCount(options);
    }

    /**
     * Gets the distinct values of the specified field name.
     *
     * @param fieldName    the field name
     * @param tResultClass the class to cast any distinct items into.
     * @return an iterable of distinct values
     * @mongodb.driver.manual reference/command/distinct/ Distinct
     */
    @Override
    public <TResult> DistinctIterable<TResult> distinct(String fieldName, Class<TResult> tResultClass) {
        return delegate.distinct(fieldName, tResultClass);
    }

    /**
     * Gets the distinct values of the specified field name.
     *
     * @param fieldName    the field name
     * @param filter       the query filter
     * @param tResultClass the class to cast any distinct items into.
     * @return an iterable of distinct values
     * @mongodb.driver.manual reference/command/distinct/ Distinct
     */
    @Override
    public <TResult> DistinctIterable<TResult> distinct(String fieldName, Bson filter, Class<TResult> tResultClass) {
        return delegate.distinct(fieldName, filter, tResultClass);
    }

    /**
     * Gets the distinct values of the specified field name.
     *
     * @param clientSession the client session with which to associate this operation
     * @param fieldName     the field name
     * @param tResultClass  the class to cast any distinct items into.
     * @return an iterable of distinct values
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual reference/command/distinct/ Distinct
     * @since 3.6
     */
    @Override
    public <TResult> DistinctIterable<TResult> distinct(ClientSession clientSession, String fieldName, Class<TResult> tResultClass) {
        return delegate.distinct(clientSession, fieldName, tResultClass);
    }

    /**
     * Gets the distinct values of the specified field name.
     *
     * @param clientSession the client session with which to associate this operation
     * @param fieldName     the field name
     * @param filter        the query filter
     * @param tResultClass  the class to cast any distinct items into.
     * @return an iterable of distinct values
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual reference/command/distinct/ Distinct
     * @since 3.6
     */
    @Override
    public <TResult> DistinctIterable<TResult> distinct(ClientSession clientSession, String fieldName, Bson filter, Class<TResult> tResultClass) {
        return delegate.distinct(clientSession, fieldName, filter, tResultClass);
    }

    /**
     * Finds all documents in the collection.
     *
     * @return the find iterable interface
     * @mongodb.driver.manual tutorial/query-documents/ Find
     */
    @Override
    public FindIterable<T> find() {
        return delegate.find();
    }

    /**
     * Finds all documents in the collection.
     *
     * @param tResultClass the class to decode each document into
     * @return the find iterable interface
     * @mongodb.driver.manual tutorial/query-documents/ Find
     */
    @Override
    public <TResult> FindIterable<TResult> find(Class<TResult> tResultClass) {
        return delegate.find(tResultClass);
    }

    /**
     * Finds all documents in the collection.
     *
     * @param filter the query filter
     * @return the find iterable interface
     * @mongodb.driver.manual tutorial/query-documents/ Find
     */
    @Override
    public FindIterable<T> find(Bson filter) {
        return delegate.find(filter);
    }

    /**
     * Finds all documents in the collection.
     *
     * @param filter       the query filter
     * @param tResultClass the class to decode each document into
     * @return the find iterable interface
     * @mongodb.driver.manual tutorial/query-documents/ Find
     */
    @Override
    public <TResult> FindIterable<TResult> find(Bson filter, Class<TResult> tResultClass) {
        return delegate.find(filter, tResultClass);
    }

    /**
     * Finds all documents in the collection.
     *
     * @param clientSession the client session with which to associate this operation
     * @return the find iterable interface
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual tutorial/query-documents/ Find
     * @since 3.6
     */
    @Override
    public FindIterable<T> find(ClientSession clientSession) {
        return delegate.find(clientSession);
    }

    /**
     * Finds all documents in the collection.
     *
     * @param clientSession the client session with which to associate this operation
     * @param tResultClass  the class to decode each document into
     * @return the find iterable interface
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual tutorial/query-documents/ Find
     * @since 3.6
     */
    @Override
    public <TResult> FindIterable<TResult> find(ClientSession clientSession, Class<TResult> tResultClass) {
        return delegate.find(clientSession, tResultClass);
    }

    /**
     * Finds all documents in the collection.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter        the query filter
     * @return the find iterable interface
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual tutorial/query-documents/ Find
     * @since 3.6
     */
    @Override
    public FindIterable<T> find(ClientSession clientSession, Bson filter) {
        return delegate.find(clientSession, filter);
    }

    /**
     * Finds all documents in the collection.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter        the query filter
     * @param tResultClass  the class to decode each document into
     * @return the find iterable interface
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual tutorial/query-documents/ Find
     * @since 3.6
     */
    @Override
    public <TResult> FindIterable<TResult> find(ClientSession clientSession, Bson filter, Class<TResult> tResultClass) {
        return delegate.find(clientSession, filter, tResultClass);
    }

    /**
     * Aggregates documents according to the specified aggregation pipeline.
     *
     * @param pipeline the aggregation pipeline
     * @return an iterable containing the result of the aggregation operation
     * @mongodb.driver.manual aggregation/ Aggregation
     * @mongodb.server.release 2.2
     */
    @Override
    public AggregateIterable<T> aggregate(List<? extends Bson> pipeline) {
        return delegate.aggregate(pipeline);
    }

    /**
     * Aggregates documents according to the specified aggregation pipeline.
     *
     * @param pipeline     the aggregation pipeline
     * @param tResultClass the class to decode each document into
     * @return an iterable containing the result of the aggregation operation
     * @mongodb.driver.manual aggregation/ Aggregation
     * @mongodb.server.release 2.2
     */
    @Override
    public <TResult> AggregateIterable<TResult> aggregate(List<? extends Bson> pipeline, Class<TResult> tResultClass) {
        return delegate.aggregate(pipeline, tResultClass);
    }

    /**
     * Aggregates documents according to the specified aggregation pipeline.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline      the aggregation pipeline
     * @return an iterable containing the result of the aggregation operation
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual aggregation/ Aggregation
     * @since 3.6
     */
    @Override
    public AggregateIterable<T> aggregate(ClientSession clientSession, List<? extends Bson> pipeline) {
        return delegate.aggregate(clientSession, pipeline);
    }

    /**
     * Aggregates documents according to the specified aggregation pipeline.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline      the aggregation pipeline
     * @param tResultClass  the class to decode each document into
     * @return an iterable containing the result of the aggregation operation
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual aggregation/ Aggregation
     * @since 3.6
     */
    @Override
    public <TResult> AggregateIterable<TResult> aggregate(ClientSession clientSession, List<? extends Bson> pipeline, Class<TResult> tResultClass) {
        return delegate.aggregate(clientSession, pipeline, tResultClass);
    }

    /**
     * Creates a change stream for this collection.
     *
     * @return the change stream iterable
     * @mongodb.driver.dochub core/changestreams Change Streams
     * @since 3.6
     */
    @Override
    public ChangeStreamIterable<T> watch() {
        return delegate.watch();
    }

    /**
     * Creates a change stream for this collection.
     *
     * @param tResultClass the class to decode each document into
     * @return the change stream iterable
     * @mongodb.driver.dochub core/changestreams Change Streams
     * @since 3.6
     */
    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(Class<TResult> tResultClass) {
        return delegate.watch(tResultClass);
    }

    /**
     * Creates a change stream for this collection.
     *
     * @param pipeline the aggregation pipeline to apply to the change stream.
     * @return the change stream iterable
     * @mongodb.driver.dochub core/changestreams Change Streams
     * @since 3.6
     */
    @Override
    public ChangeStreamIterable<T> watch(List<? extends Bson> pipeline) {
        return delegate.watch(pipeline);
    }

    /**
     * Creates a change stream for this collection.
     *
     * @param pipeline     the aggregation pipeline to apply to the change stream
     * @param tResultClass the class to decode each document into
     * @return the change stream iterable
     * @mongodb.driver.dochub core/changestreams Change Streams
     * @since 3.6
     */
    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(List<? extends Bson> pipeline, Class<TResult> tResultClass) {
        return delegate.watch(pipeline, tResultClass);
    }

    /**
     * Creates a change stream for this collection.
     *
     * @param clientSession the client session with which to associate this operation
     * @return the change stream iterable
     * @mongodb.server.release 3.6
     * @mongodb.driver.dochub core/changestreams Change Streams
     * @since 3.6
     */
    @Override
    public ChangeStreamIterable<T> watch(ClientSession clientSession) {
        return delegate.watch(clientSession);
    }

    /**
     * Creates a change stream for this collection.
     *
     * @param clientSession the client session with which to associate this operation
     * @param tResultClass  the class to decode each document into
     * @return the change stream iterable
     * @mongodb.server.release 3.6
     * @mongodb.driver.dochub core/changestreams Change Streams
     * @since 3.6
     */
    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(ClientSession clientSession, Class<TResult> tResultClass) {
        return delegate.watch(clientSession, tResultClass);
    }

    /**
     * Creates a change stream for this collection.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline      the aggregation pipeline to apply to the change stream.
     * @return the change stream iterable
     * @mongodb.server.release 3.6
     * @mongodb.driver.dochub core/changestreams Change Streams
     * @since 3.6
     */
    @Override
    public ChangeStreamIterable<T> watch(ClientSession clientSession, List<? extends Bson> pipeline) {
        return delegate.watch(clientSession, pipeline);
    }

    /**
     * Creates a change stream for this collection.
     *
     * @param clientSession the client session with which to associate this operation
     * @param pipeline      the aggregation pipeline to apply to the change stream
     * @param tResultClass  the class to decode each document into
     * @return the change stream iterable
     * @mongodb.server.release 3.6
     * @mongodb.driver.dochub core/changestreams Change Streams
     * @since 3.6
     */
    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(ClientSession clientSession, List<? extends Bson> pipeline, Class<TResult> tResultClass) {
        return delegate.watch(clientSession, pipeline, tResultClass);
    }

    /**
     * Aggregates documents according to the specified map-reduce function.
     *
     * @param mapFunction    A JavaScript function that associates or "maps" a value with a key and emits the key and value pair.
     * @param reduceFunction A JavaScript function that "reduces" to a single object all the values associated with a particular key.
     * @return an iterable containing the result of the map-reduce operation
     * @mongodb.driver.manual reference/command/mapReduce/ map-reduce
     * @deprecated Superseded by aggregate
     */
    @Override
    @Deprecated
    public MapReduceIterable<T> mapReduce(String mapFunction, String reduceFunction) {
        return delegate.mapReduce(mapFunction, reduceFunction);
    }

    /**
     * Aggregates documents according to the specified map-reduce function.
     *
     * @param mapFunction    A JavaScript function that associates or "maps" a value with a key and emits the key and value pair.
     * @param reduceFunction A JavaScript function that "reduces" to a single object all the values associated with a particular key.
     * @param tResultClass   the class to decode each resulting document into.
     * @return an iterable containing the result of the map-reduce operation
     * @mongodb.driver.manual reference/command/mapReduce/ map-reduce
     * @deprecated Superseded by aggregate
     */
    @Override
    @Deprecated
    public <TResult> MapReduceIterable<TResult> mapReduce(String mapFunction, String reduceFunction, Class<TResult> tResultClass) {
        return delegate.mapReduce(mapFunction, reduceFunction, tResultClass);
    }

    /**
     * Aggregates documents according to the specified map-reduce function.
     *
     * @param clientSession  the client session with which to associate this operation
     * @param mapFunction    A JavaScript function that associates or "maps" a value with a key and emits the key and value pair.
     * @param reduceFunction A JavaScript function that "reduces" to a single object all the values associated with a particular key.
     * @return an iterable containing the result of the map-reduce operation
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual reference/command/mapReduce/ map-reduce
     * @since 3.6
     * @deprecated Superseded by aggregate
     */
    @Override
    @Deprecated
    public MapReduceIterable<T> mapReduce(ClientSession clientSession, String mapFunction, String reduceFunction) {
        return delegate.mapReduce(clientSession, mapFunction, reduceFunction);
    }

    /**
     * Aggregates documents according to the specified map-reduce function.
     *
     * @param clientSession  the client session with which to associate this operation
     * @param mapFunction    A JavaScript function that associates or "maps" a value with a key and emits the key and value pair.
     * @param reduceFunction A JavaScript function that "reduces" to a single object all the values associated with a particular key.
     * @param tResultClass   the class to decode each resulting document into.
     * @return an iterable containing the result of the map-reduce operation
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual reference/command/mapReduce/ map-reduce
     * @since 3.6
     * @deprecated Superseded by aggregate
     */
    @Override
    @Deprecated
    public <TResult> MapReduceIterable<TResult> mapReduce(ClientSession clientSession, String mapFunction, String reduceFunction, Class<TResult> tResultClass) {
        return delegate.mapReduce(clientSession, mapFunction, reduceFunction, tResultClass);
    }

    /**
     * Executes a mix of inserts, updates, replaces, and deletes.
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.
     * The eligibility for retryable write support for bulk operations is determined on the whole bulk write. If the {@code requests}
     * contain any {@code UpdateManyModels} or {@code DeleteManyModels} then the bulk operation will not support retryable writes.</p>
     *
     * @param requests the writes to execute
     * @return the result of the bulk write
     * @throws MongoBulkWriteException if there's an exception in the bulk write operation
     * @throws MongoException          if there's an exception running the operation
     */
    @Override
    public BulkWriteResult bulkWrite(List<? extends WriteModel<? extends T>> requests) {
        return delegate.bulkWrite(requests);
    }

    /**
     * Executes a mix of inserts, updates, replaces, and deletes.
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.
     * The eligibility for retryable write support for bulk operations is determined on the whole bulk write. If the {@code requests}
     * contain any {@code UpdateManyModels} or {@code DeleteManyModels} then the bulk operation will not support retryable writes.</p>
     *
     * @param requests the writes to execute
     * @param options  the options to apply to the bulk write operation
     * @return the result of the bulk write
     * @throws MongoBulkWriteException if there's an exception in the bulk write operation
     * @throws MongoException          if there's an exception running the operation
     */
    @Override
    public BulkWriteResult bulkWrite(List<? extends WriteModel<? extends T>> requests, BulkWriteOptions options) {
        return delegate.bulkWrite(requests, options);
    }

    /**
     * Executes a mix of inserts, updates, replaces, and deletes.
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.
     * The eligibility for retryable write support for bulk operations is determined on the whole bulk write. If the {@code requests}
     * contain any {@code UpdateManyModels} or {@code DeleteManyModels} then the bulk operation will not support retryable writes.</p>
     *
     * @param clientSession the client session with which to associate this operation
     * @param requests      the writes to execute
     * @return the result of the bulk write
     * @throws MongoBulkWriteException if there's an exception in the bulk write operation
     * @throws MongoException          if there's an exception running the operation
     * @mongodb.server.release 3.6
     * @since 3.6
     */
    @Override
    public BulkWriteResult bulkWrite(ClientSession clientSession, List<? extends WriteModel<? extends T>> requests) {
        return delegate.bulkWrite(clientSession, requests);
    }

    /**
     * Executes a mix of inserts, updates, replaces, and deletes.
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.
     * The eligibility for retryable write support for bulk operations is determined on the whole bulk write. If the {@code requests}
     * contain any {@code UpdateManyModels} or {@code DeleteManyModels} then the bulk operation will not support retryable writes.</p>
     *
     * @param clientSession the client session with which to associate this operation
     * @param requests      the writes to execute
     * @param options       the options to apply to the bulk write operation
     * @return the result of the bulk write
     * @throws MongoBulkWriteException if there's an exception in the bulk write operation
     * @throws MongoException          if there's an exception running the operation
     * @mongodb.server.release 3.6
     * @since 3.6
     */
    @Override
    public BulkWriteResult bulkWrite(ClientSession clientSession, List<? extends WriteModel<? extends T>> requests, BulkWriteOptions options) {
        return delegate.bulkWrite(clientSession, requests, options);
    }

    /**
     * Inserts the provided document. If the document is missing an identifier, the driver should generate one.
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param t the document to insert
     * @return the insert one result
     * @throws MongoWriteException        if the write failed due to some specific write exception
     * @throws MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws MongoCommandException      if the write failed due to a specific command exception
     * @throws MongoException             if the write failed due some other failure
     */
    @Override
    public InsertOneResult insertOne(T t) {
        return delegate.insertOne(t);
    }

    /**
     * Inserts the provided document. If the document is missing an identifier, the driver should generate one.
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param t       the document to insert
     * @param options the options to apply to the operation
     * @return the insert one result
     * @throws MongoWriteException        if the write failed due to some specific write exception
     * @throws MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws MongoCommandException      if the write failed due to a specific command exception
     * @throws MongoException             if the write failed due some other failure
     * @since 3.2
     */
    @Override
    public InsertOneResult insertOne(T t, InsertOneOptions options) {
        return delegate.insertOne(t, options);
    }

    /**
     * Inserts the provided document. If the document is missing an identifier, the driver should generate one.
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param clientSession the client session with which to associate this operation
     * @param t             the document to insert
     * @return the insert one result
     * @throws MongoWriteException        if the write failed due to some specific write exception
     * @throws MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws MongoCommandException      if the write failed due to a specific command exception
     * @throws MongoException             if the write failed due some other failure
     * @mongodb.server.release 3.6
     * @since 3.6
     */
    @Override
    public InsertOneResult insertOne(ClientSession clientSession, T t) {
        return delegate.insertOne(clientSession, t);
    }

    /**
     * Inserts the provided document. If the document is missing an identifier, the driver should generate one.
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param clientSession the client session with which to associate this operation
     * @param t             the document to insert
     * @param options       the options to apply to the operation
     * @return the insert one result
     * @throws MongoWriteException        if the write failed due to some specific write exception
     * @throws MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws MongoCommandException      if the write failed due to a specific command exception
     * @throws MongoException             if the write failed due some other failure
     * @mongodb.server.release 3.6
     * @since 3.6
     */
    @Override
    public InsertOneResult insertOne(ClientSession clientSession, T t, InsertOneOptions options) {
        return delegate.insertOne(clientSession, t, options);
    }

    /**
     * Inserts one or more documents.  A call to this method is equivalent to a call to the {@code bulkWrite} method
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param ts the documents to insert
     * @return the insert many result
     * @throws MongoBulkWriteException  if there's an exception in the bulk write operation
     * @throws MongoCommandException    if the write failed due to a specific command exception
     * @throws MongoException           if the write failed due some other failure
     * @throws IllegalArgumentException if the documents list is null or empty, or any of the documents in the list are null
     * @see MongoCollection#bulkWrite
     */
    @Override
    public InsertManyResult insertMany(List<? extends T> ts) {
        return delegate.insertMany(ts);
    }

    /**
     * Inserts one or more documents.  A call to this method is equivalent to a call to the {@code bulkWrite} method
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param ts      the documents to insert
     * @param options the options to apply to the operation
     * @return the insert many result
     * @throws MongoBulkWriteException  if there's an exception in the bulk write operation
     * @throws MongoCommandException    if the write failed due to a specific command exception
     * @throws MongoException           if the write failed due some other failure
     * @throws IllegalArgumentException if the documents list is null or empty, or any of the documents in the list are null
     */
    @Override
    public InsertManyResult insertMany(List<? extends T> ts, InsertManyOptions options) {
        return delegate.insertMany(ts, options);
    }

    /**
     * Inserts one or more documents.  A call to this method is equivalent to a call to the {@code bulkWrite} method
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param clientSession the client session with which to associate this operation
     * @param ts            the documents to insert
     * @return the insert many result
     * @throws MongoBulkWriteException  if there's an exception in the bulk write operation
     * @throws MongoCommandException    if the write failed due to a specific command exception
     * @throws MongoException           if the write failed due some other failure
     * @throws IllegalArgumentException if the documents list is null or empty, or any of the documents in the list are null
     * @mongodb.server.release 3.6
     * @see MongoCollection#bulkWrite
     * @since 3.6
     */
    @Override
    public InsertManyResult insertMany(ClientSession clientSession, List<? extends T> ts) {
        return delegate.insertMany(clientSession, ts);
    }

    /**
     * Inserts one or more documents.  A call to this method is equivalent to a call to the {@code bulkWrite} method
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param clientSession the client session with which to associate this operation
     * @param ts            the documents to insert
     * @param options       the options to apply to the operation
     * @return the insert many result
     * @throws MongoBulkWriteException  if there's an exception in the bulk write operation
     * @throws MongoCommandException    if the write failed due to a specific command exception
     * @throws MongoException           if the write failed due some other failure
     * @throws IllegalArgumentException if the documents list is null or empty, or any of the documents in the list are null
     * @mongodb.server.release 3.6
     * @since 3.6
     */
    @Override
    public InsertManyResult insertMany(ClientSession clientSession, List<? extends T> ts, InsertManyOptions options) {
        return delegate.insertMany(clientSession, ts, options);
    }

    /**
     * Removes at most one document from the collection that matches the given filter.  If no documents match, the collection is not
     * modified.
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param filter the query filter to apply the delete operation
     * @return the result of the remove one operation
     * @throws MongoWriteException        if the write failed due to some specific write exception
     * @throws MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws MongoCommandException      if the write failed due to a specific command exception
     * @throws MongoException             if the write failed due some other failure
     */
    @Override
    public DeleteResult deleteOne(Bson filter) {
        return delegate.deleteOne(filter);
    }

    /**
     * Removes at most one document from the collection that matches the given filter.  If no documents match, the collection is not
     * modified.
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param filter  the query filter to apply the delete operation
     * @param options the options to apply to the delete operation
     * @return the result of the remove one operation
     * @throws MongoWriteException        if the write failed due to some specific write exception
     * @throws MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws MongoCommandException      if the write failed due to a specific command exception
     * @throws MongoException             if the write failed due some other failure
     * @since 3.4
     */
    @Override
    public DeleteResult deleteOne(Bson filter, DeleteOptions options) {
        return delegate.deleteOne(filter, options);
    }

    /**
     * Removes at most one document from the collection that matches the given filter.  If no documents match, the collection is not
     * modified.
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter        the query filter to apply the delete operation
     * @return the result of the remove one operation
     * @throws MongoWriteException        if the write failed due to some specific write exception
     * @throws MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws MongoCommandException      if the write failed due to a specific command exception
     * @throws MongoException             if the write failed due some other failure
     * @mongodb.server.release 3.6
     * @since 3.6
     */
    @Override
    public DeleteResult deleteOne(ClientSession clientSession, Bson filter) {
        return delegate.deleteOne(clientSession, filter);
    }

    /**
     * Removes at most one document from the collection that matches the given filter.  If no documents match, the collection is not
     * modified.
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter        the query filter to apply the delete operation
     * @param options       the options to apply to the delete operation
     * @return the result of the remove one operation
     * @throws MongoWriteException        if the write failed due to some specific write exception
     * @throws MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws MongoCommandException      if the write failed due to a specific command exception
     * @throws MongoException             if the write failed due some other failure
     * @mongodb.server.release 3.6
     * @since 3.6
     */
    @Override
    public DeleteResult deleteOne(ClientSession clientSession, Bson filter, DeleteOptions options) {
        return delegate.deleteOne(clientSession, filter, options);
    }

    /**
     * Removes all documents from the collection that match the given query filter.  If no documents match, the collection is not modified.
     *
     * @param filter the query filter to apply the delete operation
     * @return the result of the remove many operation
     * @throws MongoWriteException        if the write failed due to some specific write exception
     * @throws MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws MongoCommandException      if the write failed due to a specific command exception
     * @throws MongoException             if the write failed due some other failure
     */
    @Override
    public DeleteResult deleteMany(Bson filter) {
        return delegate.deleteMany(filter);
    }

    /**
     * Removes all documents from the collection that match the given query filter.  If no documents match, the collection is not modified.
     *
     * @param filter  the query filter to apply the delete operation
     * @param options the options to apply to the delete operation
     * @return the result of the remove many operation
     * @throws MongoWriteException        if the write failed due to some specific write exception
     * @throws MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws MongoCommandException      if the write failed due to a specific command exception
     * @throws MongoException             if the write failed due some other failure
     * @since 3.4
     */
    @Override
    public DeleteResult deleteMany(Bson filter, DeleteOptions options) {
        return delegate.deleteMany(filter, options);
    }

    /**
     * Removes all documents from the collection that match the given query filter.  If no documents match, the collection is not modified.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter        the query filter to apply the delete operation
     * @return the result of the remove many operation
     * @throws MongoWriteException        if the write failed due to some specific write exception
     * @throws MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws MongoCommandException      if the write failed due to a specific command exception
     * @throws MongoException             if the write failed due some other failure
     * @mongodb.server.release 3.6
     * @since 3.6
     */
    @Override
    public DeleteResult deleteMany(ClientSession clientSession, Bson filter) {
        return delegate.deleteMany(clientSession, filter);
    }

    /**
     * Removes all documents from the collection that match the given query filter.  If no documents match, the collection is not modified.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter        the query filter to apply the delete operation
     * @param options       the options to apply to the delete operation
     * @return the result of the remove many operation
     * @throws MongoWriteException        if the write failed due to some specific write exception
     * @throws MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws MongoCommandException      if the write failed due to a specific command exception
     * @throws MongoException             if the write failed due some other failure
     * @mongodb.server.release 3.6
     * @since 3.6
     */
    @Override
    public DeleteResult deleteMany(ClientSession clientSession, Bson filter, DeleteOptions options) {
        return delegate.deleteMany(clientSession, filter, options);
    }

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
     * @throws MongoWriteException        if the write failed due to some specific write exception
     * @throws MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws MongoCommandException      if the write failed due to a specific command exception
     * @throws MongoException             if the write failed due some other failure
     * @mongodb.driver.manual tutorial/modify-documents/#replace-the-document Replace
     * @mongodb.driver.manual reference/command/update   Update Command Behaviors
     */
    @Override
    public UpdateResult replaceOne(Bson filter, T replacement) {
        return delegate.replaceOne(filter, replacement);
    }

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
     * @throws MongoWriteException        if the write failed due to some specific write exception
     * @throws MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws MongoCommandException      if the write failed due to a specific command exception
     * @throws MongoException             if the write failed due some other failure
     * @mongodb.driver.manual tutorial/modify-documents/#replace-the-document Replace
     * @mongodb.driver.manual reference/command/update   Update Command Behaviors
     * @since 3.7
     */
    @Override
    public UpdateResult replaceOne(Bson filter, T replacement, ReplaceOptions replaceOptions) {
        return delegate.replaceOne(filter, replacement, replaceOptions);
    }

    /**
     * Replace a document in the collection according to the specified arguments.
     *
     * <p>Use this method to replace a document using the specified replacement argument. To update the document with update operators, use
     * the corresponding {@link #updateOne(ClientSession, Bson, Bson)} method.</p>
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter        the query filter to apply the replace operation
     * @param replacement   the replacement document
     * @return the result of the replace one operation
     * @throws MongoWriteException        if the write failed due to some specific write exception
     * @throws MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws MongoCommandException      if the write failed due to a specific command exception
     * @throws MongoException             if the write failed due some other failure
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual tutorial/modify-documents/#replace-the-document Replace
     * @mongodb.driver.manual reference/command/update   Update Command Behaviors
     * @since 3.6
     */
    @Override
    public UpdateResult replaceOne(ClientSession clientSession, Bson filter, T replacement) {
        return delegate.replaceOne(clientSession, filter, replacement);
    }

    /**
     * Replace a document in the collection according to the specified arguments.
     *
     * <p>Use this method to replace a document using the specified replacement argument. To update the document with update operators, use
     * the corresponding {@link #updateOne(ClientSession, Bson, Bson, UpdateOptions)} method.</p>
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param clientSession  the client session with which to associate this operation
     * @param filter         the query filter to apply the replace operation
     * @param replacement    the replacement document
     * @param replaceOptions the options to apply to the replace operation
     * @return the result of the replace one operation
     * @throws MongoWriteException        if the write failed due to some specific write exception
     * @throws MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws MongoCommandException      if the write failed due to a specific command exception
     * @throws MongoException             if the write failed due some other failure
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual tutorial/modify-documents/#replace-the-document Replace
     * @mongodb.driver.manual reference/command/update   Update Command Behaviors
     * @since 3.7
     */
    @Override
    public UpdateResult replaceOne(ClientSession clientSession, Bson filter, T replacement, ReplaceOptions replaceOptions) {
        return delegate.replaceOne(clientSession, filter, replacement, replaceOptions);
    }

    /**
     * Update a single document in the collection according to the specified arguments.
     *
     * <p>Use this method to only update the corresponding fields in the document according to the update operators used in the update
     * document. To replace the entire document with a new document, use the corresponding {@link #replaceOne(Bson, Object)} method.</p>
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param filter a document describing the query filter, which may not be null.
     * @param update a document describing the update, which may not be null. The update to apply must include at least one update operator.
     * @return the result of the update one operation
     * @throws MongoWriteException        if the write failed due to some specific write exception
     * @throws MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws MongoCommandException      if the write failed due to a specific command exception
     * @throws MongoException             if the write failed due some other failure
     * @mongodb.driver.manual tutorial/modify-documents/ Updates
     * @mongodb.driver.manual reference/operator/update/ Update Operators
     * @mongodb.driver.manual reference/command/update   Update Command Behaviors
     * @see MongoCollection#replaceOne(Bson, Object)
     */
    @Override
    public UpdateResult updateOne(Bson filter, Bson update) {
        return delegate.updateOne(filter, update);
    }

    /**
     * Update a single document in the collection according to the specified arguments.
     *
     * <p>Use this method to only update the corresponding fields in the document according to the update operators used in the update
     * document. To replace the entire document with a new document, use the corresponding {@link #replaceOne(Bson, Object, ReplaceOptions)}
     * method.</p>
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param filter        a document describing the query filter, which may not be null.
     * @param update        a document describing the update, which may not be null. The update to apply must include at least one update
     *                      operator.
     * @param updateOptions the options to apply to the update operation
     * @return the result of the update one operation
     * @throws MongoWriteException        if the write failed due to some specific write exception
     * @throws MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws MongoCommandException      if the write failed due to a specific command exception
     * @throws MongoException             if the write failed due some other failure
     * @mongodb.driver.manual tutorial/modify-documents/ Updates
     * @mongodb.driver.manual reference/operator/update/ Update Operators
     * @mongodb.driver.manual reference/command/update   Update Command Behaviors
     * @see MongoCollection#replaceOne(Bson, Object, ReplaceOptions)
     */
    @Override
    public UpdateResult updateOne(Bson filter, Bson update, UpdateOptions updateOptions) {
        return delegate.updateOne(filter, update, updateOptions);
    }

    /**
     * Update a single document in the collection according to the specified arguments.
     *
     * <p>Use this method to only update the corresponding fields in the document according to the update operators used in the update
     * document. To replace the entire document with a new document, use the corresponding {@link #replaceOne(ClientSession, Bson, Object)}
     * method.</p>
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter        a document describing the query filter, which may not be null.
     * @param update        a document describing the update, which may not be null. The update to apply must include at least one update operator.
     * @return the result of the update one operation
     * @throws MongoWriteException        if the write failed due to some specific write exception
     * @throws MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws MongoCommandException      if the write failed due to a specific command exception
     * @throws MongoException             if the write failed due some other failure
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual tutorial/modify-documents/ Updates
     * @mongodb.driver.manual reference/operator/update/ Update Operators
     * @mongodb.driver.manual reference/command/update   Update Command Behaviors
     * @see MongoCollection#replaceOne(ClientSession, Bson, Object)
     * @since 3.6
     */
    @Override
    public UpdateResult updateOne(ClientSession clientSession, Bson filter, Bson update) {
        return delegate.updateOne(clientSession, filter, update);
    }

    /**
     * Update a single document in the collection according to the specified arguments.
     *
     * <p>Use this method to only update the corresponding fields in the document according to the update operators used in the update
     * document. To replace the entire document with a new document, use the corresponding {@link #replaceOne(ClientSession, Bson, Object,
     * ReplaceOptions)} method.</p>
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter        a document describing the query filter, which may not be null.
     * @param update        a document describing the update, which may not be null. The update to apply must include at least one update
     *                      operator.
     * @param updateOptions the options to apply to the update operation
     * @return the result of the update one operation
     * @throws MongoWriteException        if the write failed due to some specific write exception
     * @throws MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws MongoCommandException      if the write failed due to a specific command exception
     * @throws MongoException             if the write failed due some other failure
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual tutorial/modify-documents/ Updates
     * @mongodb.driver.manual reference/operator/update/ Update Operators
     * @mongodb.driver.manual reference/command/update   Update Command Behaviors
     * @see MongoCollection#replaceOne(ClientSession, Bson, Object, ReplaceOptions)
     * @since 3.6
     */
    @Override
    public UpdateResult updateOne(ClientSession clientSession, Bson filter, Bson update, UpdateOptions updateOptions) {
        return delegate.updateOne(clientSession, filter, update, updateOptions);
    }

    /**
     * Update a single document in the collection according to the specified arguments.
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param filter a document describing the query filter, which may not be null.
     * @param update a pipeline describing the update, which may not be null.
     * @return the result of the update one operation
     * @throws MongoWriteException        if the write failed due some other failure specific to the update command
     * @throws MongoWriteConcernException if the write failed due being unable to fulfil the write concern
     * @throws MongoException             if the write failed due some other failure
     * @mongodb.server.release 4.2
     * @mongodb.driver.manual tutorial/modify-documents/ Updates
     * @mongodb.driver.manual reference/operator/update/ Update Operators
     * @since 3.11
     */
    @Override
    public UpdateResult updateOne(Bson filter, List<? extends Bson> update) {
        return delegate.updateOne(filter, update);
    }

    /**
     * Update a single document in the collection according to the specified arguments.
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param filter        a document describing the query filter, which may not be null.
     * @param update        a pipeline describing the update, which may not be null.
     * @param updateOptions the options to apply to the update operation
     * @return the result of the update one operation
     * @throws MongoWriteException        if the write failed due some other failure specific to the update command
     * @throws MongoWriteConcernException if the write failed due being unable to fulfil the write concern
     * @throws MongoException             if the write failed due some other failure
     * @mongodb.server.release 4.2
     * @mongodb.driver.manual tutorial/modify-documents/ Updates
     * @mongodb.driver.manual reference/operator/update/ Update Operators
     * @since 3.11
     */
    @Override
    public UpdateResult updateOne(Bson filter, List<? extends Bson> update, UpdateOptions updateOptions) {
        return delegate.updateOne(filter, update, updateOptions);
    }

    /**
     * Update a single document in the collection according to the specified arguments.
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter        a document describing the query filter, which may not be null.
     * @param update        a pipeline describing the update, which may not be null.
     * @return the result of the update one operation
     * @throws MongoWriteException        if the write failed due some other failure specific to the update command
     * @throws MongoWriteConcernException if the write failed due being unable to fulfil the write concern
     * @throws MongoException             if the write failed due some other failure
     * @mongodb.server.release 4.2
     * @mongodb.driver.manual tutorial/modify-documents/ Updates
     * @mongodb.driver.manual reference/operator/update/ Update Operators
     * @since 3.11
     */
    @Override
    public UpdateResult updateOne(ClientSession clientSession, Bson filter, List<? extends Bson> update) {
        return delegate.updateOne(clientSession, filter, update);
    }

    /**
     * Update a single document in the collection according to the specified arguments.
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter        a document describing the query filter, which may not be null.
     * @param update        a pipeline describing the update, which may not be null.
     * @param updateOptions the options to apply to the update operation
     * @return the result of the update one operation
     * @throws MongoWriteException        if the write failed due some other failure specific to the update command
     * @throws MongoWriteConcernException if the write failed due being unable to fulfil the write concern
     * @throws MongoException             if the write failed due some other failure
     * @mongodb.server.release 4.2
     * @mongodb.driver.manual tutorial/modify-documents/ Updates
     * @mongodb.driver.manual reference/operator/update/ Update Operators
     * @since 3.11
     */
    @Override
    public UpdateResult updateOne(ClientSession clientSession, Bson filter, List<? extends Bson> update, UpdateOptions updateOptions) {
        return delegate.updateOne(clientSession, filter, update, updateOptions);
    }

    /**
     * Update all documents in the collection according to the specified arguments.
     *
     * @param filter a document describing the query filter, which may not be null.
     * @param update a document describing the update, which may not be null. The update to apply must include only update operators.
     * @return the result of the update many operation
     * @throws MongoWriteException        if the write failed due to some specific write exception
     * @throws MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws MongoCommandException      if the write failed due to a specific command exception
     * @throws MongoException             if the write failed due some other failure
     * @mongodb.driver.manual tutorial/modify-documents/ Updates
     * @mongodb.driver.manual reference/operator/update/ Update Operators
     */
    @Override
    public UpdateResult updateMany(Bson filter, Bson update) {
        return delegate.updateMany(filter, update);
    }

    /**
     * Update all documents in the collection according to the specified arguments.
     *
     * @param filter        a document describing the query filter, which may not be null.
     * @param update        a document describing the update, which may not be null. The update to apply must include only update operators.
     * @param updateOptions the options to apply to the update operation
     * @return the result of the update many operation
     * @throws MongoWriteException        if the write failed due to some specific write exception
     * @throws MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws MongoCommandException      if the write failed due to a specific command exception
     * @throws MongoException             if the write failed due some other failure
     * @mongodb.driver.manual tutorial/modify-documents/ Updates
     * @mongodb.driver.manual reference/operator/update/ Update Operators
     */
    @Override
    public UpdateResult updateMany(Bson filter, Bson update, UpdateOptions updateOptions) {
        return delegate.updateMany(filter, update, updateOptions);
    }

    /**
     * Update all documents in the collection according to the specified arguments.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter        a document describing the query filter, which may not be null.
     * @param update        a document describing the update, which may not be null. The update to apply must include only update operators.
     * @return the result of the update many operation
     * @throws MongoWriteException        if the write failed due to some specific write exception
     * @throws MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws MongoCommandException      if the write failed due to a specific command exception
     * @throws MongoException             if the write failed due some other failure
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual tutorial/modify-documents/ Updates
     * @mongodb.driver.manual reference/operator/update/ Update Operators
     * @since 3.6
     */
    @Override
    public UpdateResult updateMany(ClientSession clientSession, Bson filter, Bson update) {
        return delegate.updateMany(clientSession, filter, update);
    }

    /**
     * Update all documents in the collection according to the specified arguments.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter        a document describing the query filter, which may not be null.
     * @param update        a document describing the update, which may not be null. The update to apply must include only update operators.
     * @param updateOptions the options to apply to the update operation
     * @return the result of the update many operation
     * @throws MongoWriteException        if the write failed due to some specific write exception
     * @throws MongoWriteConcernException if the write failed due to being unable to fulfil the write concern
     * @throws MongoCommandException      if the write failed due to a specific command exception
     * @throws MongoException             if the write failed due some other failure
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual tutorial/modify-documents/ Updates
     * @mongodb.driver.manual reference/operator/update/ Update Operators
     * @since 3.6
     */
    @Override
    public UpdateResult updateMany(ClientSession clientSession, Bson filter, Bson update, UpdateOptions updateOptions) {
        return delegate.updateMany(clientSession, filter, update, updateOptions);
    }

    /**
     * Update all documents in the collection according to the specified arguments.
     *
     * @param filter a document describing the query filter, which may not be null.
     * @param update a pipeline describing the update, which may not be null.
     * @return the result of the update many operation
     * @throws MongoWriteException        if the write failed due some other failure specific to the update command
     * @throws MongoWriteConcernException if the write failed due being unable to fulfil the write concern
     * @throws MongoException             if the write failed due some other failure
     * @mongodb.server.release 4.2
     * @mongodb.driver.manual tutorial/modify-documents/ Updates
     * @mongodb.driver.manual reference/operator/update/ Update Operators
     * @since 3.11
     */
    @Override
    public UpdateResult updateMany(Bson filter, List<? extends Bson> update) {
        return delegate.updateMany(filter, update);
    }

    /**
     * Update all documents in the collection according to the specified arguments.
     *
     * @param filter        a document describing the query filter, which may not be null.
     * @param update        a pipeline describing the update, which may not be null.
     * @param updateOptions the options to apply to the update operation
     * @return the result of the update many operation
     * @throws MongoWriteException        if the write failed due some other failure specific to the update command
     * @throws MongoWriteConcernException if the write failed due being unable to fulfil the write concern
     * @throws MongoException             if the write failed due some other failure
     * @mongodb.server.release 4.2
     * @mongodb.driver.manual tutorial/modify-documents/ Updates
     * @mongodb.driver.manual reference/operator/update/ Update Operators
     * @since 3.11
     */
    @Override
    public UpdateResult updateMany(Bson filter, List<? extends Bson> update, UpdateOptions updateOptions) {
        return delegate.updateMany(filter, update, updateOptions);
    }

    /**
     * Update all documents in the collection according to the specified arguments.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter        a document describing the query filter, which may not be null.
     * @param update        a pipeline describing the update, which may not be null.
     * @return the result of the update many operation
     * @throws MongoWriteException        if the write failed due some other failure specific to the update command
     * @throws MongoWriteConcernException if the write failed due being unable to fulfil the write concern
     * @throws MongoException             if the write failed due some other failure
     * @mongodb.server.release 4.2
     * @mongodb.driver.manual tutorial/modify-documents/ Updates
     * @mongodb.driver.manual reference/operator/update/ Update Operators
     * @since 3.11
     */
    @Override
    public UpdateResult updateMany(ClientSession clientSession, Bson filter, List<? extends Bson> update) {
        return delegate.updateMany(clientSession, filter, update);
    }

    /**
     * Update all documents in the collection according to the specified arguments.
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter        a document describing the query filter, which may not be null.
     * @param update        a pipeline describing the update, which may not be null.
     * @param updateOptions the options to apply to the update operation
     * @return the result of the update many operation
     * @throws MongoWriteException        if the write failed due some other failure specific to the update command
     * @throws MongoWriteConcernException if the write failed due being unable to fulfil the write concern
     * @throws MongoException             if the write failed due some other failure
     * @mongodb.server.release 4.2
     * @mongodb.driver.manual tutorial/modify-documents/ Updates
     * @mongodb.driver.manual reference/operator/update/ Update Operators
     * @since 3.11
     */
    @Override
    public UpdateResult updateMany(ClientSession clientSession, Bson filter, List<? extends Bson> update, UpdateOptions updateOptions) {
        return delegate.updateMany(clientSession, filter, update, updateOptions);
    }

    /**
     * Atomically find a document and remove it.
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param filter the query filter to find the document with
     * @return the document that was removed.  If no documents matched the query filter, then null will be returned
     */
    @Override
    @Nullable
    public T findOneAndDelete(Bson filter) {
        return delegate.findOneAndDelete(filter);
    }

    /**
     * Atomically find a document and remove it.
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param filter  the query filter to find the document with
     * @param options the options to apply to the operation
     * @return the document that was removed.  If no documents matched the query filter, then null will be returned
     */
    @Override
    @Nullable
    public T findOneAndDelete(Bson filter, FindOneAndDeleteOptions options) {
        return delegate.findOneAndDelete(filter, options);
    }

    /**
     * Atomically find a document and remove it.
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter        the query filter to find the document with
     * @return the document that was removed.  If no documents matched the query filter, then null will be returned
     * @mongodb.server.release 3.6
     * @since 3.6
     */
    @Override
    @Nullable
    public T findOneAndDelete(ClientSession clientSession, Bson filter) {
        return delegate.findOneAndDelete(clientSession, filter);
    }

    /**
     * Atomically find a document and remove it.
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter        the query filter to find the document with
     * @param options       the options to apply to the operation
     * @return the document that was removed.  If no documents matched the query filter, then null will be returned
     * @mongodb.server.release 3.6
     * @since 3.6
     */
    @Override
    @Nullable
    public T findOneAndDelete(ClientSession clientSession, Bson filter, FindOneAndDeleteOptions options) {
        return delegate.findOneAndDelete(clientSession, filter, options);
    }

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
     * @mongodb.driver.manual reference/command/update   Update Command Behaviors
     */
    @Override
    @Nullable
    public T findOneAndReplace(Bson filter, T replacement) {
        return delegate.findOneAndReplace(filter, replacement);
    }

    /**
     * Atomically find a document and replace it.
     *
     * <p>Use this method to replace a document using the specified replacement argument. To update the document with update operators, use
     * the corresponding {@link #findOneAndUpdate(Bson, Bson, FindOneAndUpdateOptions)} method.</p>
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param filter      the query filter to apply the replace operation
     * @param replacement the replacement document
     * @param options     the options to apply to the operation
     * @return the document that was replaced.  Depending on the value of the {@code returnOriginal} property, this will either be the
     * document as it was before the update or as it is after the update.  If no documents matched the query filter, then null will be
     * returned
     * @mongodb.driver.manual reference/command/update   Update Command Behaviors
     */
    @Override
    @Nullable
    public T findOneAndReplace(Bson filter, T replacement, FindOneAndReplaceOptions options) {
        return delegate.findOneAndReplace(filter, replacement, options);
    }

    /**
     * Atomically find a document and replace it.
     *
     * <p>Use this method to replace a document using the specified replacement argument. To update the document with update operators, use
     * the corresponding {@link #findOneAndUpdate(ClientSession, Bson, Bson)} method.</p>
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter        the query filter to apply the replace operation
     * @param replacement   the replacement document
     * @return the document that was replaced.  Depending on the value of the {@code returnOriginal} property, this will either be the
     * document as it was before the update or as it is after the update.  If no documents matched the query filter, then null will be
     * returned
     * @mongodb.driver.manual reference/command/update   Update Command Behaviors
     * @mongodb.server.release 3.6
     * @since 3.6
     */
    @Override
    @Nullable
    public T findOneAndReplace(ClientSession clientSession, Bson filter, T replacement) {
        return delegate.findOneAndReplace(clientSession, filter, replacement);
    }

    /**
     * Atomically find a document and replace it.
     *
     * <p>Use this method to replace a document using the specified replacement argument. To update the document with update operators, use
     * the corresponding {@link #findOneAndUpdate(ClientSession, Bson, Bson, FindOneAndUpdateOptions)} method.</p>
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter        the query filter to apply the replace operation
     * @param replacement   the replacement document
     * @param options       the options to apply to the operation
     * @return the document that was replaced.  Depending on the value of the {@code returnOriginal} property, this will either be the
     * document as it was before the update or as it is after the update.  If no documents matched the query filter, then null will be
     * returned
     * @mongodb.driver.manual reference/command/update   Update Command Behaviors
     * @mongodb.server.release 3.6
     * @since 3.6
     */
    @Override
    @Nullable
    public T findOneAndReplace(ClientSession clientSession, Bson filter, T replacement, FindOneAndReplaceOptions options) {
        return delegate.findOneAndReplace(clientSession, filter, replacement, options);
    }

    /**
     * Atomically find a document and update it.
     *
     * <p>Use this method to only update the corresponding fields in the document according to the update operators used in the update
     * document. To replace the entire document with a new document, use the corresponding {@link #findOneAndReplace(Bson, Object)} method.
     * </p>
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param filter a document describing the query filter, which may not be null.
     * @param update a document describing the update, which may not be null. The update to apply must include at least one update operator.
     * @return the document that was updated before the update was applied.  If no documents matched the query filter, then null will be
     * returned
     * @mongodb.driver.manual reference/command/update   Update Command Behaviors
     * @see MongoCollection#findOneAndReplace(Bson, Object)
     */
    @Override
    @Nullable
    public T findOneAndUpdate(Bson filter, Bson update) {
        return delegate.findOneAndUpdate(filter, update);
    }

    /**
     * Atomically find a document and update it.
     *
     * <p>Use this method to only update the corresponding fields in the document according to the update operators used in the update
     * document. To replace the entire document with a new document, use the corresponding {@link #findOneAndReplace(Bson, Object,
     * FindOneAndReplaceOptions)} method.</p>
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
     * @mongodb.driver.manual reference/command/update   Update Command Behaviors
     * @see MongoCollection#findOneAndReplace(Bson, Object, FindOneAndReplaceOptions)
     */
    @Override
    @Nullable
    public T findOneAndUpdate(Bson filter, Bson update, FindOneAndUpdateOptions options) {
        return delegate.findOneAndUpdate(filter, update, options);
    }

    /**
     * Atomically find a document and update it.
     *
     * <p>Use this method to only update the corresponding fields in the document according to the update operators used in the update
     * document. To replace the entire document with a new document, use the corresponding {@link #findOneAndReplace(ClientSession, Bson,
     * Object)} method.</p>
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter        a document describing the query filter, which may not be null.
     * @param update        a document describing the update, which may not be null. The update to apply must include at least one update operator.
     * @return the document that was updated before the update was applied.  If no documents matched the query filter, then null will be
     * returned
     * @mongodb.driver.manual reference/command/update   Update Command Behaviors
     * @mongodb.server.release 3.6
     * @see MongoCollection#findOneAndReplace(ClientSession, Bson, Object)
     * @since 3.6
     */
    @Override
    @Nullable
    public T findOneAndUpdate(ClientSession clientSession, Bson filter, Bson update) {
        return delegate.findOneAndUpdate(clientSession, filter, update);
    }

    /**
     * Atomically find a document and update it.
     *
     * <p>Use this method to only update the corresponding fields in the document according to the update operators used in the update
     * document. To replace the entire document with a new document, use the corresponding {@link #findOneAndReplace(ClientSession, Bson,
     * Object, FindOneAndReplaceOptions)} method.</p>
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter        a document describing the query filter, which may not be null.
     * @param update        a document describing the update, which may not be null. The update to apply must include at least one update
     *                      operator.
     * @param options       the options to apply to the operation
     * @return the document that was updated.  Depending on the value of the {@code returnOriginal} property, this will either be the
     * document as it was before the update or as it is after the update.  If no documents matched the query filter, then null will be
     * returned
     * @mongodb.driver.manual reference/command/update   Update Command Behaviors
     * @mongodb.server.release 3.6
     * @see MongoCollection#findOneAndReplace(ClientSession, Bson, Object, FindOneAndReplaceOptions)
     * @since 3.6
     */
    @Override
    @Nullable
    public T findOneAndUpdate(ClientSession clientSession, Bson filter, Bson update, FindOneAndUpdateOptions options) {
        return delegate.findOneAndUpdate(clientSession, filter, update, options);
    }

    /**
     * Atomically find a document and update it.
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param filter a document describing the query filter, which may not be null.
     * @param update a pipeline describing the update, which may not be null.
     * @return the document that was updated before the update was applied.  If no documents matched the query filter, then null will be
     * returned
     * @mongodb.server.release 4.2
     * @since 3.11
     */
    @Override
    @Nullable
    public T findOneAndUpdate(Bson filter, List<? extends Bson> update) {
        return delegate.findOneAndUpdate(filter, update);
    }

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
     * @mongodb.server.release 4.2
     * @since 3.11
     */
    @Override
    @Nullable
    public T findOneAndUpdate(Bson filter, List<? extends Bson> update, FindOneAndUpdateOptions options) {
        return delegate.findOneAndUpdate(filter, update, options);
    }

    /**
     * Atomically find a document and update it.
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter        a document describing the query filter, which may not be null.
     * @param update        a pipeline describing the update, which may not be null.
     * @return the document that was updated before the update was applied.  If no documents matched the query filter, then null will be
     * returned
     * @mongodb.server.release 4.2
     * @since 3.11
     */
    @Override
    @Nullable
    public T findOneAndUpdate(ClientSession clientSession, Bson filter, List<? extends Bson> update) {
        return delegate.findOneAndUpdate(clientSession, filter, update);
    }

    /**
     * Atomically find a document and update it.
     *
     * <p>Note: Supports retryable writes on MongoDB server versions 3.6 or higher when the retryWrites setting is enabled.</p>
     *
     * @param clientSession the client session with which to associate this operation
     * @param filter        a document describing the query filter, which may not be null.
     * @param update        a pipeline describing the update, which may not be null.
     * @param options       the options to apply to the operation
     * @return the document that was updated.  Depending on the value of the {@code returnOriginal} property, this will either be the
     * document as it was before the update or as it is after the update.  If no documents matched the query filter, then null will be
     * returned
     * @mongodb.server.release 4.2
     * @since 3.11
     */
    @Override
    @Nullable
    public T findOneAndUpdate(ClientSession clientSession, Bson filter, List<? extends Bson> update, FindOneAndUpdateOptions options) {
        return delegate.findOneAndUpdate(clientSession, filter, update, options);
    }

    /**
     * Drops this collection from the Database.
     *
     * @mongodb.driver.manual reference/command/drop/ Drop Collection
     */
    @Override
    public void drop() {
        delegate.drop();
    }

    /**
     * Drops this collection from the Database.
     *
     * @param clientSession the client session with which to associate this operation
     * @mongodb.driver.manual reference/command/drop/ Drop Collection
     * @mongodb.server.release 3.6
     * @since 3.6
     */
    @Override
    public void drop(ClientSession clientSession) {
        delegate.drop(clientSession);
    }

    /**
     * Drops this collection from the Database.
     *
     * @param dropCollectionOptions various options for dropping the collection
     * @mongodb.driver.manual reference/command/drop/ Drop Collection
     * @mongodb.server.release 6.0
     * @since 4.7
     */
    @Override
    public void drop(DropCollectionOptions dropCollectionOptions) {
        delegate.drop(dropCollectionOptions);
    }

    /**
     * Drops this collection from the Database.
     *
     * @param clientSession         the client session with which to associate this operation
     * @param dropCollectionOptions various options for dropping the collection
     * @mongodb.driver.manual reference/command/drop/ Drop Collection
     * @mongodb.server.release 6.0
     * @since 4.7
     */
    @Override
    public void drop(ClientSession clientSession, DropCollectionOptions dropCollectionOptions) {
        delegate.drop(clientSession, dropCollectionOptions);
    }

    /**
     * Create an Atlas Search index for the collection.
     *
     * @param indexName  the name of the search index to create.
     * @param definition the search index mapping definition.
     * @return the search index name.
     * @mongodb.server.release 7.0
     * @mongodb.driver.manual reference/command/createSearchIndexes/ Create Search indexes
     * @since 4.11
     */
    @Override
    public String createSearchIndex(String indexName, Bson definition) {
        return delegate.createSearchIndex(indexName, definition);
    }

    /**
     * Create an Atlas Search index with {@code "default"} name for the collection.
     *
     * @param definition the search index mapping definition.
     * @return the search index name.
     * @mongodb.server.release 7.0
     * @mongodb.driver.manual reference/command/createSearchIndexes/ Create Search indexes
     * @since 4.11
     */
    @Override
    public String createSearchIndex(Bson definition) {
        return delegate.createSearchIndex(definition);
    }

    /**
     * Create one or more Atlas Search indexes for the collection.
     * <p>
     * The name can be omitted for a single index, in which case a name will be {@code "default"}.
     * </p>
     *
     * @param searchIndexModels the search index models.
     * @return the search index names in the order specified by the given list of {@link SearchIndexModel}s.
     * @mongodb.server.release 7.0
     * @mongodb.driver.manual reference/command/createSearchIndexes/ Create Search indexes
     * @since 4.11
     */
    @Override
    public List<String> createSearchIndexes(List<SearchIndexModel> searchIndexModels) {
        return delegate.createSearchIndexes(searchIndexModels);
    }

    /**
     * Update an Atlas Search index in the collection.
     *
     * @param indexName  the name of the search index to update.
     * @param definition the search index mapping definition.
     * @mongodb.server.release 7.0
     * @mongodb.driver.manual reference/command/updateSearchIndex/ Update Search index
     * @since 4.11
     */
    @Override
    public void updateSearchIndex(String indexName, Bson definition) {
        delegate.updateSearchIndex(indexName, definition);
    }

    /**
     * Drop an Atlas Search index given its name.
     *
     * @param indexName the name of the search index to drop.
     * @mongodb.server.release 7.0
     * @mongodb.driver.manual reference/command/dropSearchIndex/ Drop Search index
     * @since 4.11
     */
    @Override
    public void dropSearchIndex(String indexName) {
        delegate.dropSearchIndex(indexName);
    }

    /**
     * Get all Atlas Search indexes in this collection.
     *
     * @return the list search indexes iterable interface.
     * @mongodb.server.release 7.0
     * @since 4.11
     */
    @Override
    public ListSearchIndexesIterable<Document> listSearchIndexes() {
        return delegate.listSearchIndexes();
    }

    /**
     * Get all Atlas Search indexes in this collection.
     *
     * @param tResultClass the class to decode each document into.
     * @return the list search indexes iterable interface.
     * @mongodb.server.release 7.0
     * @since 4.11
     */
    @Override
    public <TResult> ListSearchIndexesIterable<TResult> listSearchIndexes(Class<TResult> tResultClass) {
        return delegate.listSearchIndexes(tResultClass);
    }

    /**
     * Create an index with the given keys.
     *
     * @param keys an object describing the index key(s), which may not be null.
     * @return the index name
     * @mongodb.driver.manual reference/command/createIndexes Create indexes
     */
    @Override
    public String createIndex(Bson keys) {
        return delegate.createIndex(keys);
    }

    /**
     * Create an index with the given keys and options.
     *
     * @param keys         an object describing the index key(s), which may not be null.
     * @param indexOptions the options for the index
     * @return the index name
     * @mongodb.driver.manual reference/command/createIndexes Create indexes
     */
    @Override
    public String createIndex(Bson keys, IndexOptions indexOptions) {
        return delegate.createIndex(keys, indexOptions);
    }

    /**
     * Create an index with the given keys.
     *
     * @param clientSession the client session with which to associate this operation
     * @param keys          an object describing the index key(s), which may not be null.
     * @return the index name
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual reference/command/createIndexes Create indexes
     * @since 3.6
     */
    @Override
    public String createIndex(ClientSession clientSession, Bson keys) {
        return delegate.createIndex(clientSession, keys);
    }

    /**
     * Create an index with the given keys and options.
     *
     * @param clientSession the client session with which to associate this operation
     * @param keys          an object describing the index key(s), which may not be null.
     * @param indexOptions  the options for the index
     * @return the index name
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual reference/command/createIndexes Create indexes
     * @since 3.6
     */
    @Override
    public String createIndex(ClientSession clientSession, Bson keys, IndexOptions indexOptions) {
        return delegate.createIndex(clientSession, keys, indexOptions);
    }

    /**
     * Create multiple indexes.
     *
     * @param indexes the list of indexes
     * @return the list of index names
     * @mongodb.driver.manual reference/command/createIndexes Create indexes
     */
    @Override
    public List<String> createIndexes(List<IndexModel> indexes) {
        return delegate.createIndexes(indexes);
    }

    /**
     * Create multiple indexes.
     *
     * @param indexes            the list of indexes
     * @param createIndexOptions options to use when creating indexes
     * @return the list of index names
     * @mongodb.driver.manual reference/command/createIndexes Create indexes
     * @since 3.6
     */
    @Override
    public List<String> createIndexes(List<IndexModel> indexes, CreateIndexOptions createIndexOptions) {
        return delegate.createIndexes(indexes, createIndexOptions);
    }

    /**
     * Create multiple indexes.
     *
     * @param clientSession the client session with which to associate this operation
     * @param indexes       the list of indexes
     * @return the list of index names
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual reference/command/createIndexes Create indexes
     * @since 3.6
     */
    @Override
    public List<String> createIndexes(ClientSession clientSession, List<IndexModel> indexes) {
        return delegate.createIndexes(clientSession, indexes);
    }

    /**
     * Create multiple indexes.
     *
     * @param clientSession      the client session with which to associate this operation
     * @param indexes            the list of indexes
     * @param createIndexOptions options to use when creating indexes
     * @return the list of index names
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual reference/command/createIndexes Create indexes
     * @since 3.6
     */
    @Override
    public List<String> createIndexes(ClientSession clientSession, List<IndexModel> indexes, CreateIndexOptions createIndexOptions) {
        return delegate.createIndexes(clientSession, indexes, createIndexOptions);
    }

    /**
     * Get all the indexes in this collection.
     *
     * @return the list indexes iterable interface
     * @mongodb.driver.manual reference/command/listIndexes/ List indexes
     */
    @Override
    public ListIndexesIterable<Document> listIndexes() {
        return delegate.listIndexes();
    }

    /**
     * Get all the indexes in this collection.
     *
     * @param tResultClass the class to decode each document into
     * @return the list indexes iterable interface
     * @mongodb.driver.manual reference/command/listIndexes/ List indexes
     */
    @Override
    public <TResult> ListIndexesIterable<TResult> listIndexes(Class<TResult> tResultClass) {
        return delegate.listIndexes(tResultClass);
    }

    /**
     * Get all the indexes in this collection.
     *
     * @param clientSession the client session with which to associate this operation
     * @return the list indexes iterable interface
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual reference/command/listIndexes/ List indexes
     * @since 3.6
     */
    @Override
    public ListIndexesIterable<Document> listIndexes(ClientSession clientSession) {
        return delegate.listIndexes(clientSession);
    }

    /**
     * Get all the indexes in this collection.
     *
     * @param clientSession the client session with which to associate this operation
     * @param tResultClass  the class to decode each document into
     * @return the list indexes iterable interface
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual reference/command/listIndexes/ List indexes
     * @since 3.6
     */
    @Override
    public <TResult> ListIndexesIterable<TResult> listIndexes(ClientSession clientSession, Class<TResult> tResultClass) {
        return delegate.listIndexes(clientSession, tResultClass);
    }

    /**
     * Drops the index given its name.
     *
     * @param indexName the name of the index to remove
     * @mongodb.driver.manual reference/command/dropIndexes/ Drop indexes
     */
    @Override
    public void dropIndex(String indexName) {
        delegate.dropIndex(indexName);
    }

    /**
     * Drops the index given its name.
     *
     * @param indexName        the name of the index to remove
     * @param dropIndexOptions the options to use when dropping indexes
     * @mongodb.driver.manual reference/command/dropIndexes/ Drop indexes
     * @since 3.6
     */
    @Override
    public void dropIndex(String indexName, DropIndexOptions dropIndexOptions) {
        delegate.dropIndex(indexName, dropIndexOptions);
    }

    /**
     * Drops the index given the keys used to create it.
     *
     * @param keys the keys of the index to remove
     * @mongodb.driver.manual reference/command/dropIndexes/ Drop indexes
     */
    @Override
    public void dropIndex(Bson keys) {
        delegate.dropIndex(keys);
    }

    /**
     * Drops the index given the keys used to create it.
     *
     * @param keys             the keys of the index to remove
     * @param dropIndexOptions the options to use when dropping indexes
     * @mongodb.driver.manual reference/command/dropIndexes/ Drop indexes
     * @since 3.6
     */
    @Override
    public void dropIndex(Bson keys, DropIndexOptions dropIndexOptions) {
        delegate.dropIndex(keys, dropIndexOptions);
    }

    /**
     * Drops the index given its name.
     *
     * @param clientSession the client session with which to associate this operation
     * @param indexName     the name of the index to remove
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual reference/command/dropIndexes/ Drop indexes
     * @since 3.6
     */
    @Override
    public void dropIndex(ClientSession clientSession, String indexName) {
        delegate.dropIndex(clientSession, indexName);
    }

    /**
     * Drops the index given the keys used to create it.
     *
     * @param clientSession the client session with which to associate this operation
     * @param keys          the keys of the index to remove
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual reference/command/dropIndexes/ Drop indexes
     * @since 3.6
     */
    @Override
    public void dropIndex(ClientSession clientSession, Bson keys) {
        delegate.dropIndex(clientSession, keys);
    }

    /**
     * Drops the index given its name.
     *
     * @param clientSession    the client session with which to associate this operation
     * @param indexName        the name of the index to remove
     * @param dropIndexOptions the options to use when dropping indexes
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual reference/command/dropIndexes/ Drop indexes
     * @since 3.6
     */
    @Override
    public void dropIndex(ClientSession clientSession, String indexName, DropIndexOptions dropIndexOptions) {
        delegate.dropIndex(clientSession, indexName, dropIndexOptions);
    }

    /**
     * Drops the index given the keys used to create it.
     *
     * @param clientSession    the client session with which to associate this operation
     * @param keys             the keys of the index to remove
     * @param dropIndexOptions the options to use when dropping indexes
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual reference/command/dropIndexes/ Drop indexes
     * @since 3.6
     */
    @Override
    public void dropIndex(ClientSession clientSession, Bson keys, DropIndexOptions dropIndexOptions) {
        delegate.dropIndex(clientSession, keys, dropIndexOptions);
    }

    /**
     * Drop all the indexes on this collection, except for the default on _id.
     *
     * @mongodb.driver.manual reference/command/dropIndexes/ Drop indexes
     */
    @Override
    public void dropIndexes() {
        delegate.dropIndexes();
    }

    /**
     * Drop all the indexes on this collection, except for the default on _id.
     *
     * @param clientSession the client session with which to associate this operation
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual reference/command/dropIndexes/ Drop indexes
     * @since 3.6
     */
    @Override
    public void dropIndexes(ClientSession clientSession) {
        delegate.dropIndexes(clientSession);
    }

    /**
     * Drop all the indexes on this collection, except for the default on _id.
     *
     * @param dropIndexOptions the options to use when dropping indexes
     * @mongodb.driver.manual reference/command/dropIndexes/ Drop indexes
     * @since 3.6
     */
    @Override
    public void dropIndexes(DropIndexOptions dropIndexOptions) {
        delegate.dropIndexes(dropIndexOptions);
    }

    /**
     * Drop all the indexes on this collection, except for the default on _id.
     *
     * @param clientSession    the client session with which to associate this operation
     * @param dropIndexOptions the options to use when dropping indexes
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual reference/command/dropIndexes/ Drop indexes
     * @since 3.6
     */
    @Override
    public void dropIndexes(ClientSession clientSession, DropIndexOptions dropIndexOptions) {
        delegate.dropIndexes(clientSession, dropIndexOptions);
    }

    /**
     * Rename the collection with oldCollectionName to the newCollectionName.
     *
     * @param newCollectionNamespace the namespace the collection will be renamed to
     * @throws MongoServerException if you provide a newCollectionName that is the name of an existing collection, or if the
     *                              oldCollectionName is the name of a collection that doesn't exist
     * @mongodb.driver.manual reference/command/renameCollection Rename collection
     */
    @Override
    public void renameCollection(MongoNamespace newCollectionNamespace) {
        delegate.renameCollection(newCollectionNamespace);
    }

    /**
     * Rename the collection with oldCollectionName to the newCollectionName.
     *
     * @param newCollectionNamespace  the name the collection will be renamed to
     * @param renameCollectionOptions the options for renaming a collection
     * @throws MongoServerException if you provide a newCollectionName that is the name of an existing collection and dropTarget
     *                              is false, or if the oldCollectionName is the name of a collection that doesn't exist
     * @mongodb.driver.manual reference/command/renameCollection Rename collection
     */
    @Override
    public void renameCollection(MongoNamespace newCollectionNamespace, RenameCollectionOptions renameCollectionOptions) {
        delegate.renameCollection(newCollectionNamespace, renameCollectionOptions);
    }

    /**
     * Rename the collection with oldCollectionName to the newCollectionName.
     *
     * @param clientSession          the client session with which to associate this operation
     * @param newCollectionNamespace the namespace the collection will be renamed to
     * @throws MongoServerException if you provide a newCollectionName that is the name of an existing collection, or if the
     *                              oldCollectionName is the name of a collection that doesn't exist
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual reference/command/renameCollection Rename collection
     * @since 3.6
     */
    @Override
    public void renameCollection(ClientSession clientSession, MongoNamespace newCollectionNamespace) {
        delegate.renameCollection(clientSession, newCollectionNamespace);
    }

    /**
     * Rename the collection with oldCollectionName to the newCollectionName.
     *
     * @param clientSession           the client session with which to associate this operation
     * @param newCollectionNamespace  the name the collection will be renamed to
     * @param renameCollectionOptions the options for renaming a collection
     * @throws MongoServerException if you provide a newCollectionName that is the name of an existing collection and dropTarget
     *                              is false, or if the oldCollectionName is the name of a collection that doesn't exist
     * @mongodb.server.release 3.6
     * @mongodb.driver.manual reference/command/renameCollection Rename collection
     * @since 3.6
     */
    @Override
    public void renameCollection(ClientSession clientSession, MongoNamespace newCollectionNamespace, RenameCollectionOptions renameCollectionOptions) {
        delegate.renameCollection(clientSession, newCollectionNamespace, renameCollectionOptions);
    }
}
