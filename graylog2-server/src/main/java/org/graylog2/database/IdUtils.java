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
package org.graylog2.database;

import com.mongodb.client.result.InsertOneResult;
import jakarta.annotation.Nullable;
import org.bson.BsonObjectId;
import org.bson.BsonValue;
import org.bson.types.ObjectId;

import java.util.Optional;

public class IdUtils {

    private IdUtils() {
    }

    @Nullable
    public static ObjectId insertedId(InsertOneResult result) {
        return Optional.ofNullable(result.getInsertedId())
                .map(BsonValue::asObjectId)
                .map(BsonObjectId::getValue)
                .orElse(null);
    }

    @Nullable
    public static String insertedIdAsString(InsertOneResult result) {
        return Optional.ofNullable(insertedId(result))
                .map(ObjectId::toHexString)
                .orElse(null);
    }
}

