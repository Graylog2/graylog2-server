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
package org.graylog2.database.jackson.legacy;

import com.mongodb.client.result.InsertOneResult;
import org.bson.BsonValue;
import org.mongojack.JacksonMongoCollection;
import org.mongojack.WriteResult;

/**
 * Compatibility layer to support existing code interacting with the Mongojack 2.x API.
 *
 * @deprecated use {@link org.graylog2.database.MongoCollections} as an entrypoint for interacting with MongoDB.
 */
@Deprecated
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
        final BsonValue insertedId = insertOneResult.getInsertedId();
        if (insertedId == null) {
            return null;
        }
        return collection.findOneById(insertedId);
    }

    @Override
    public int getN() {
        return 0;
    }

    @Override
    public boolean wasAcknowledged() {
        return insertOneResult.wasAcknowledged();
    }

    @Override
    public K getSavedId() {
        return WriteResult.toIdType(insertOneResult.getInsertedId(), idType);
    }

    @Override
    public boolean isUpdateOfExisting() {
        return false;
    }
}
