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
package org.mongojack;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoException;
import com.mongodb.MongoServerException;
import com.mongodb.WriteConcern;
import com.mongodb.WriteConcernResult;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationStrength;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.UpdateOptions;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.UuidRepresentation;
import org.bson.codecs.CollectibleCodec;
import org.bson.conversions.Bson;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class JacksonDBCollection<T, K> {

    private final JacksonMongoCollection<T> delegate;
    private final Class<T> valueType;
    private final Class<K> idType;
    private final ObjectMapper objectMapper;
    private final DBCollection dbCollection;

    public static <T, K> JacksonDBCollection<T, K> wrap(
            DBCollection dbCollection, Class<T> type, Class<K> keyType,
            ObjectMapper objectMapper) {

        return new JacksonDBCollection<>(dbCollection, type, keyType, objectMapper);
    }

    private JacksonDBCollection(DBCollection dbCollection, Class<T> valueType, Class<K> idType, ObjectMapper objectMapper) {

        final MongoDatabase db = dbCollection.getDB().getMongoClient().getDatabase(dbCollection.getDB().getName());

        this.dbCollection = dbCollection;
        this.delegate = JacksonMongoCollection.builder()
                .withObjectMapper(objectMapper)
                .build(db, dbCollection.getName(), valueType, UuidRepresentation.UNSPECIFIED);
        this.valueType = valueType;
        this.idType = idType;
        this.objectMapper = objectMapper;
    }

    public void createIndex(DBObject keys, DBObject options) {
        delegate.createIndex(new BasicDBObject(keys.toMap()), toIndexOptions(options));
    }

    public void createIndex(DBObject keys) {
        delegate.createIndex(new BasicDBObject(keys.toMap()));
    }

    public DBCursor<T> find() {
        return new DBCursor<>(delegate, null, delegate::find);
    }

    public DBCursor<T> find(Bson filter) {
        return new DBCursor<>(delegate, filter, () -> delegate.find(filter));
    }

    public DBCursor<T> find(DBObject dbObject) {
        final BasicDBObject filter = new BasicDBObject(dbObject.toMap());
        return new DBCursor<>(delegate, filter, () -> delegate.find(filter));
    }

    public T findOneById(K objectId) {
        return delegate.findOneById(objectId);
    }

    public T findOne(Bson filter) throws MongoException {
        return delegate.findOne(filter);
    }

    public T findOne(DBObject filter) throws MongoException {
        return delegate.findOne(new BasicDBObject(filter.toMap()));
    }

    public T findOne() {
        return delegate.findOne();
    }

    public <TResult> Iterable<TResult> distinct(String fieldName, Class<TResult> tResultClass) {
        return delegate.distinct(fieldName, tResultClass);
    }

    public long count() {
        return delegate.estimatedDocumentCount();
    }

    public long count(Bson filter) {
        return delegate.countDocuments(filter);
    }

    public long count(DBObject dbObject) {
        return delegate.countDocuments(new BasicDBObject(dbObject.toMap()));
    }

    public WriteResult<T, K> save(T object) {
        return save(object, null);
    }

    public WriteResult<T, K> save(T object, WriteConcern concern) {
        return doSave(object, concern);
    }

    private WriteResult<T, K> doSave(T object, WriteConcern concern) {
        final var collection = concern == null ? delegate : delegate.withWriteConcern(concern);

        final CollectibleCodec<T> codec = (CollectibleCodec<T>) delegate.getCodecRegistry().get(valueType);
        final BsonValue id = codec.getDocumentId(object);

        try {
            if (id == null || id.isNull()) {
                return new LegacyInsertOneResult<>(collection, collection.insertOne(object), idType);
            } else {
                final var idQuery = Filters.eq("_id", id);
                return new LegacyUpdateOneResult<>(collection, object,
                        collection.replaceOne(idQuery, object, new ReplaceOptions().upsert(true)), valueType, idType);
            }
        } catch (MongoServerException e) {
            throw possiblyAsDuplicateKeyError(e);
        }
    }

    public LegacyDeleteResult<T, K> remove(DBObject query) {
        return new LegacyDeleteResult<>(delegate, delegate.deleteMany(new BasicDBObject(query.toMap())));
    }

    public LegacyDeleteResult<T, K> remove(Bson filter) {
        return new LegacyDeleteResult<>(delegate, delegate.deleteMany(filter));
    }

    public WriteResult<T, K> remove(Bson filter, WriteConcern concern) {
        var coll = delegate.withWriteConcern(concern);
        return new LegacyDeleteResult<>(coll, coll.deleteMany(filter));
    }

    public LegacyDeleteResult<T, K> removeById(K objectId) {
        return new LegacyDeleteResult<>(delegate, delegate.removeById(objectId));
    }

    public WriteResult<T, K> update(Bson filter, T object, boolean upsert, boolean multi) {
        return update(filter, object, upsert, multi, null);
    }

    public WriteResult<T, K> update(Bson filter, T object, boolean upsert, boolean multi,
                                    @Nullable WriteConcern concern) {
        if (multi) {
            throw new IllegalArgumentException(("Multi-update ist not supported for object-based updates."));
        }
        final var coll = concern == null ? delegate : delegate.withWriteConcern(concern);
        final var options = new ReplaceOptions().upsert(upsert);
        try {
            return new LegacyUpdateOneResult<>(coll, object, coll.replaceOne(filter, object, options), valueType, idType);
        } catch (MongoServerException e) {
            throw possiblyAsDuplicateKeyError(e);
        }
    }

    public WriteResult<T, K> update(Bson filter, Bson update, boolean upsert, boolean multi) {
        try {
            if (multi) {
                return new LegacyUpdateResult<>(delegate,
                        delegate.updateMany(filter, update, new UpdateOptions().upsert(upsert)), idType);
            } else {
                return new LegacyUpdateResult<>(delegate,
                        delegate.updateOne(filter, update, new UpdateOptions().upsert(upsert)), idType);
            }
        } catch (MongoServerException e) {
            throw possiblyAsDuplicateKeyError(e);
        }
    }

    public WriteResult<T, K> update(Bson query, Bson update) {
        return update(query, update, false, false);
    }

    public WriteResult<T, K> update(DBObject query, Bson update) {
        return update((Bson) new BasicDBObject(query.toMap()), update);
    }

    public WriteResult<T, K> update(Bson query, T object) {
        return update(query, object, false, false);
    }

    public void updateById(K id, Bson update) {
        try {
            delegate.updateById(id, update);
        } catch (MongoServerException e) {
            throw possiblyAsDuplicateKeyError(e);
        }
    }

    public WriteResult<T, K> updateById(K id, T update) {
        try {
            return new LegacyUpdateOneResult<>(delegate, update, delegate.replaceOneById(id, update), valueType, idType);
        } catch (MongoServerException e) {
            throw possiblyAsDuplicateKeyError(e);
        }
    }

    public WriteResult<T, K> updateMulti(Bson query, Bson update) {
        return update(query, update, false, true);
    }

    public WriteResult<T, K> insert(T object) {
        try {
            return new LegacyInsertOneResult<>(delegate, delegate.insertOne(object), idType);
        } catch (MongoServerException e) {
            throw possiblyAsDuplicateKeyError(e);
        }
    }

    public void insert(List<T> list) {
        try {
            delegate.insert(list);
        } catch (MongoServerException e) {
            throw possiblyAsDuplicateKeyError(e);
        }
    }

    public long getCount() {
        return delegate.countDocuments();
    }

    public long getCount(Bson filter) {
        return delegate.countDocuments(filter);
    }

    public T findAndModify(Bson filter, Bson fields, Bson sort, boolean remove, Bson update, boolean returnNew,
                           boolean upsert) {
        if (remove) {
            throw new IllegalArgumentException("Removing objects is not supported!");
        }

        var options = new FindOneAndUpdateOptions()
                .projection(fields)
                .sort(sort)
                .returnDocument(returnNew ? ReturnDocument.AFTER : ReturnDocument.BEFORE)
                .upsert(upsert);

        try {
            return delegate.findOneAndUpdate(filter, update, options);
        } catch (MongoServerException e) {
            throw possiblyAsDuplicateKeyError(e);
        }
    }

    public T findAndModify(Bson query, Bson update) {
        return findAndModify(query, null, null, false, update, false, false);
    }

    public T findAndModify(Bson filter, Bson fields, Bson sort, boolean remove, T object, boolean returnNew, boolean upsert) {
        if (remove) {
            throw new IllegalArgumentException("Removing objects is not supported!");
        }
        var options = new FindOneAndReplaceOptions()
                .projection(fields)
                .sort(sort)
                .returnDocument(returnNew ? ReturnDocument.AFTER : ReturnDocument.BEFORE)
                .upsert(upsert);

        try {
            return delegate.findOneAndReplace(filter, object, options);
        } catch (MongoServerException e) {
            throw possiblyAsDuplicateKeyError(e);
        }
    }

    public T findAndRemove(Bson filter) {
        return delegate.findOneAndDelete(filter);
    }

    public void dropIndexes() {
        delegate.dropIndexes();
    }

    public void dropIndex(Bson keys) {
        delegate.dropIndex(keys);
    }

    public void dropIndex(String name) {
        delegate.dropIndex(name);
    }

    public void drop() {
        delegate.drop();
    }

    public List<DBObject> getIndexInfo() {
        return delegate.listIndexes(DBObject.class).into(new ArrayList<>());
    }

    public DBCollection getDbCollection() {
        return dbCollection;
    }

    record IndexOptionDto(
            @JsonProperty("unique") Optional<Boolean> unique,
            @JsonProperty("name") Optional<String> name,
            @JsonProperty("expireAfterSeconds") Optional<Long> expireAfterSeconds,
            @JsonProperty("sparse") Optional<Boolean> sparse,
            @JsonProperty("collation") Optional<CollationDto> collationDto) {

        public IndexOptions toIndexOptions() {
            final var io = new IndexOptions();
            unique.ifPresent(io::unique);
            name.ifPresent(io::name);
            expireAfterSeconds.ifPresent(seconds -> io.expireAfter(seconds, TimeUnit.SECONDS));
            sparse.ifPresent(io::sparse);
            collationDto.ifPresent(collation -> io.collation(collation.toCollation()));
            return io;
        }
    }

    record CollationDto(
            @JsonProperty("locale") Optional<String> locale,
            @JsonProperty("strength") Optional<Integer> strength
    ) {
        public Collation toCollation() {
            final var builder = Collation.builder();
            locale.ifPresent(builder::locale);
            strength.ifPresent(s -> builder.collationStrength(CollationStrength.fromInt(s)));
            return builder.build();
        }
    }

    private IndexOptions toIndexOptions(DBObject options) {
        return objectMapper.convertValue(options.toMap(), IndexOptionDto.class).toIndexOptions();
    }

    private MongoException possiblyAsDuplicateKeyError(final MongoServerException e) {
        if (ErrorCategory.fromErrorCode(e.getCode()) == ErrorCategory.DUPLICATE_KEY) {
            return new DuplicateKeyException(new BsonDocument("err", new BsonString(e.getMessage())),
                    e.getServerAddress(),
                    WriteConcernResult.acknowledged(0, false, null));
        } else {
            return e;
        }
    }

}
