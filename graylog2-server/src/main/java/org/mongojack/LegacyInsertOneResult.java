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
