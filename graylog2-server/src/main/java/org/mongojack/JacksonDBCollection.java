package org.mongojack;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationStrength;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.UpdateOptions;
import org.bson.UuidRepresentation;
import org.bson.conversions.Bson;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class JacksonDBCollection<T, K> {

    private final JacksonMongoCollection<T> delegate;
    private final Class<K> idType;
    private final ObjectMapper objectMapper;

    public static <T, K> JacksonDBCollection<T, K> wrap(
            DBCollection dbCollection, Class<T> type, Class<K> keyType,
            ObjectMapper objectMapper) {

        final MongoDatabase db = dbCollection.getDB().getMongoClient().getDatabase(dbCollection.getName());

        final JacksonMongoCollection<T> jacksonMongoCollection = JacksonMongoCollection.builder()
                .withObjectMapper(objectMapper)
                .build(db, dbCollection.getName(), type, UuidRepresentation.UNSPECIFIED);

        return new JacksonDBCollection<>(jacksonMongoCollection, keyType, objectMapper);
    }

    private JacksonDBCollection(JacksonMongoCollection<T> delegate, Class<K> idType, ObjectMapper objectMapper) {
        this.delegate = delegate;
        this.idType = idType;
        this.objectMapper = objectMapper;
    }

    // TODO: can we accept Bson here?
    public void createIndex(DBObject keys, DBObject options) {
        delegate.createIndex(new BasicDBObject(keys.toMap()), toIndexOptions(options));
    }

    public void createIndex(DBObject keys) {
        delegate.createIndex(new BasicDBObject(keys.toMap()));
    }

    public void createIndex(Bson keys, @Nullable String name, boolean unique) {
        delegate.createIndex(keys, new IndexOptions().name(name).unique(unique));
    }

    public DBCursor<T> find() {
        return new DBCursor<>(delegate);
    }

    public DBCursor<T> find(Bson filter) {
        return new DBCursor<>(delegate, filter);
    }

    public DBCursor<T> find(DBObject dbObject) {
        return new DBCursor<>(delegate, new BasicDBObject(dbObject.toMap()));
    }

    public T findOneById(K objectId) {
        return delegate.findOneById(objectId);
    }

    public T findOne(Bson filter) throws MongoException {
        return delegate.findOne(filter);
    }

    // TODO: check if this works
    public Iterable<?> distinct(String fieldName) {
        return delegate.distinct(fieldName, BasicDBObject.class);
    }

    public long count() {
        return delegate.estimatedDocumentCount();
    }

    public long count(Bson filter) {
        return delegate.countDocuments(filter);
    }

    public WriteResult<T, K> save(T object) {
        return new LegacyUpdateResult<>(delegate, delegate.save(object), idType);
    }

    public WriteResult<T, K> save(T object, WriteConcern concern) {
        return new LegacyUpdateResult<>(delegate, delegate.save(object, concern), idType);
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
        return new LegacyUpdateResult<>(coll, coll.replaceOne(filter, object, options), idType);
    }

    public WriteResult<T, K> update(Bson filter, Bson update, boolean upsert, boolean multi) {
        if (multi) {
            return new LegacyUpdateResult<>(delegate,
                    delegate.updateMany(filter, update, new UpdateOptions().upsert(upsert)), idType);
        } else {
            return new LegacyUpdateResult<>(delegate,
                    delegate.updateOne(filter, update, new UpdateOptions().upsert(upsert)), idType);
        }
    }

    public WriteResult<T, K> update(Bson query, Bson update) {
        return update(query, update, false, false);
    }

    public WriteResult<T, K> update(Bson query, T object) {
        return update(query, object, false, false);
    }

    public void updateById(K id, Bson update) {
        delegate.updateById(id, update);
    }

    public WriteResult<T, K> updateById(K id, T update) {
        return new LegacyUpdateResult<>(delegate, delegate.replaceOneById(id, update), idType);
    }

    public WriteResult<T, K> updateMulti(Bson query, Bson update) {
        return update(query, update, false, true);
    }

    public WriteResult<T, K> insert(T object) {
        return new LegacyInsertOneResult<>(delegate, delegate.insertOne(object), idType);
    }

    public void insert(List<T> list) {
        delegate.insert(list);
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

        return delegate.findOneAndUpdate(filter, update, options);
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

        return delegate.findOneAndReplace(filter, object, options);
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

    record IndexOptionDto(
            @JsonProperty("unique") Optional<Boolean> unique,
            @JsonProperty("name") Optional<String> name,
            @JsonProperty("expireAfterSeconds") Optional<Long> expireAfterSeconds,
            @JsonProperty("collation") Optional<CollationDto> collationDto) {

        public IndexOptions toIndexOptions() {
            final var io = new IndexOptions();
            unique.ifPresent(io::unique);
            name.ifPresent(io::name);
            expireAfterSeconds.ifPresent(seconds -> io.expireAfter(seconds, TimeUnit.SECONDS));
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
//        final var indexOptions = new IndexOptions();
//        for (final String key : options.keySet()) {
//            final Object value = options.get(key);
//            switch (key) {
//                case "unique" -> indexOptions.unique(Boolean.parseBoolean(value.toString()));
//                case "name" -> indexOptions.name(value.toString());
//                case "expireAfterSeconds" ->
//                        indexOptions.expireAfter(Long.parseLong(value.toString()), TimeUnit.SECONDS);
//                case "collation" -> indexOptions.collation(toCollation(value));
//            }
//
//            //new BasicDBObject("locale", "en").append("strength", 2))
//            //this.db.createIndex(new BasicDBObject(sortField, 1), new BasicDBObject(COLLATION_KEY, new BasicDBObject(LOCALE_KEY, "en")));
//
//        }
        return objectMapper.convertValue(options.toMap(), IndexOptionDto.class).toIndexOptions();
    }

}
