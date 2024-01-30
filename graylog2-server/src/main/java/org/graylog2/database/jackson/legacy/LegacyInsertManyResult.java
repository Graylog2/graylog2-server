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

import com.mongodb.MongoException;
import com.mongodb.client.result.InsertManyResult;
import org.bson.BsonValue;
import org.mongojack.JacksonMongoCollection;
import org.mongojack.WriteResult;

/**
 * Compatibility layer to support existing code interacting with the Mongojack 2.x API.
 *
 * @deprecated use {@link org.graylog2.database.MongoCollections} as an entrypoint for interacting with MongoDB.
 */
@Deprecated
public class LegacyInsertManyResult<T, K> implements WriteResult<T, K> {
    private final JacksonMongoCollection<T> collection;
    private final InsertManyResult insertOneResult;
    private final Class<K> idType;

    public LegacyInsertManyResult(JacksonMongoCollection<T> collection, InsertManyResult insertManyResult,
                                  Class<K> idType) {
        this.collection = collection;
        this.insertOneResult = insertManyResult;
        this.idType = idType;
    }

    @Override
    public T getSavedObject() {
        final BsonValue firstId = getFirstId();

        if (firstId != null) {
            final T item = collection.findOneById(firstId);
            if (item != null) {
                return item;
            }
        }

        throw new MongoException("No objects to return");
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
        final BsonValue firstId = getFirstId();

        if (firstId != null) {
            return WriteResult.toIdType(firstId, idType);
        }
        throw new MongoException("No objects to return");
    }

    @Override
    public boolean isUpdateOfExisting() {
        return false;
    }

    private BsonValue getFirstId() {
        return insertOneResult.getInsertedIds().get(0);
    }
}
