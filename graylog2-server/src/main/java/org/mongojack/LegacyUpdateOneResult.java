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

import com.google.common.primitives.Ints;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonValue;
import org.bson.codecs.CollectibleCodec;

public class LegacyUpdateOneResult<T, K> implements WriteResult<T, K> {
    protected final JacksonMongoCollection<T> collection;
    private final T object;
    private final UpdateResult updateResult;
    private final Class<T> valueType;
    private final Class<K> idType;

    public LegacyUpdateOneResult(JacksonMongoCollection<T> collection, T object, UpdateResult updateResult, Class<T> valueType, Class<K> idType) {
        this.collection = collection;
        this.object = object;
        this.updateResult = updateResult;
        this.valueType = valueType;
        this.idType = idType;
    }

    @Override
    public T getSavedObject() {
        return object;
    }

    @Override
    public K getSavedId() {
        final CollectibleCodec<T> codec = (CollectibleCodec<T>) collection.getCodecRegistry().get(valueType);
        final BsonValue id = codec.getDocumentId(object);

        return WriteResult.toIdType(id, idType);
    }

    @Override
    public int getN() {
        return (updateResult.getUpsertedId() != null) ? 1 : Ints.saturatedCast(updateResult.getMatchedCount());
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
        return updateResult.getUpsertedId() == null && updateResult.getModifiedCount() > 0;
    }
}
