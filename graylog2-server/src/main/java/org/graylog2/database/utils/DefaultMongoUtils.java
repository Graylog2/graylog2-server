package org.graylog2.database.utils;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.types.ObjectId;

import java.util.Optional;

public class DefaultMongoUtils<T> implements MongoUtils<T> {
    private final MongoCollection<T> collection;

    public DefaultMongoUtils(MongoCollection<T> delegate) {
        this.collection = delegate;
    }

    @Override
    public Optional<T> getById(ObjectId id) {
        return Optional.ofNullable(collection.find(Filters.eq("_id", id)).first());
    }

    @Override
    public Optional<T> getById(String id) {
        return getById(new ObjectId(id));
    }

    @Override
    public boolean deleteById(ObjectId id) {
        return collection.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
    }

    @Override
    public boolean deleteById(String id) {
        return deleteById(new ObjectId(id));
    }

}
