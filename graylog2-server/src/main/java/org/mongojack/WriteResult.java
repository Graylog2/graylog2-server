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

import com.mongodb.MongoException;
import org.bson.BsonValue;
import org.bson.types.ObjectId;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Compatibility layer to support existing code interacting with the Mongojack 2.x API.
 *
 * @deprecated use {@link org.graylog2.database.MongoCollections} as an entrypoint for interacting with MongoDB.
 */
@Deprecated
public interface WriteResult<T, K> {
    default T getSavedObject() {
        throw new MongoException("No objects to return");
    }

    int getN();

    boolean wasAcknowledged();

    default K getSavedId() {
        throw new MongoException("No Id to return");
    }

    default Object getUpsertedId() {
        throw new MongoException("No Id to return");
    }

    default boolean isUpdateOfExisting() {
        throw new MongoException("No update information available");
    }

    @SuppressWarnings("unchecked")
    static <L> L toIdType(BsonValue id, Class<L> idType) {
        if (id == null) {
            return null;
        }
        if (String.class.isAssignableFrom(idType)) {
            if (id.isObjectId()) {
                return (L) id.asObjectId().getValue().toHexString();
            }
            if (id.isString()) {
                return (L) id.asString().getValue();
            }
        }
        if (ObjectId.class.isAssignableFrom(idType)) {
            return (L) id.asObjectId().getValue();
        }
        throw new IllegalArgumentException(f("Only String and ObjectID types supported for ID. Got %s.",
                id.getBsonType()));
    }
}
