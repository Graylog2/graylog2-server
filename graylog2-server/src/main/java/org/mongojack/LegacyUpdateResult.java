package org.mongojack;

import com.google.common.primitives.Ints;
import com.mongodb.client.result.UpdateResult;

public class LegacyUpdateResult<T, K> implements WriteResult<T, K> {
    protected final JacksonMongoCollection<T> collection;
    private final UpdateResult updateResult;
    private final Class<K> idType;

    public LegacyUpdateResult(JacksonMongoCollection<T> collection, UpdateResult updateResult, Class<K> idType) {
        this.collection = collection;
        this.updateResult = updateResult;
        this.idType = idType;
    }

    @Override
    public int getN() {
        return (getUpsertedId() != null) ? 1 : Ints.saturatedCast(updateResult.getMatchedCount());
    }

    @Override
    public boolean wasAcknowledged() {
        return updateResult.wasAcknowledged();
    }

    @Override
    public Object getUpsertedId() {
        return WriteResult.toIdType(updateResult.getUpsertedId(), idType);
    }

    @Override
    public boolean isUpdateOfExisting() {
        return updateResult.getUpsertedId() == null;
    }
}
