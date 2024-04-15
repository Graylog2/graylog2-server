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

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import org.mongojack.Id;
import org.mongojack.ObjectId;

/**
 * Common interface for entities stored in MongoDB.
 */
public interface MongoEntity {

    /**
     * ID of the entity. Will be stored as field "_id" with type ObjectId in MongoDB.
     *
     * @return Hex string representation of the entity's ID
     */
    @Nullable
    @ObjectId
    @Id
    @JsonProperty("id")
    String id();
}
