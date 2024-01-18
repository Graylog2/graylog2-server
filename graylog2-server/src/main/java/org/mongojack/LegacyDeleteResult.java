package org.mongojack;

import com.google.common.primitives.Ints;
import com.mongodb.client.result.DeleteResult;

public class LegacyDeleteResult<T, K> implements WriteResult<T, K> {
    protected final JacksonMongoCollection<T> collection;
    private final DeleteResult deleteResult;

    public LegacyDeleteResult(JacksonMongoCollection<T> collection, DeleteResult deleteResult) {
        this.collection = collection;
        this.deleteResult = deleteResult;
    }

    @Override
    public int getN() {
        return Ints.saturatedCast(deleteResult.getDeletedCount());
    }

    @Override
    public boolean wasAcknowledged() {
        return deleteResult.wasAcknowledged();
    }
}
