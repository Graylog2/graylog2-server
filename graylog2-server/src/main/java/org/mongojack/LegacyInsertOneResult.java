package org.mongojack;

import com.mongodb.client.result.InsertOneResult;

public class LegacyInsertOneResult<T, K> implements WriteResult<T, K> {
    private final JacksonMongoCollection<T> collection;
    private final InsertOneResult insertOneResult;
    private final Class<K> idType;

    public LegacyInsertOneResult(JacksonMongoCollection<T> collection, InsertOneResult insertOneResult,
                                 Class<K> idType) {
        this.collection = collection;
        this.insertOneResult = insertOneResult;
        this.idType = idType;
    }

    @Override
    public T getSavedObject() {
        return collection.findOneById(insertOneResult.getInsertedId());
    }

    @Override
    public int getN() {
        return 1;
    }

    @Override
    public boolean wasAcknowledged() {
        return insertOneResult.wasAcknowledged();
    }

    @Override
    public K getSavedId() {
        return WriteResult.toIdType(insertOneResult.getInsertedId(), idType);
    }
}
