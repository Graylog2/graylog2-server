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
