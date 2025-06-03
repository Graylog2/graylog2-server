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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import org.mongojack.Id;
import org.mongojack.ObjectId;

/**
 * Common interface for entities stored in MongoDB.
 */
public interface MongoEntity {
    String FIELD_METADATA = "_metadata";

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

    // TODO: Eventually, we want the metadata to be serialized for every entity. But for a transition period, we
    //  will only serialize it for entities that explicitly handle metadata by overriding this method. After the
    //  transition period, the default implementation for #metadata should be removed and replaced with a method
    //  that looks somewhat like this:
    //
    // @JsonProperty(FIELD_METADATA)
    // MongoEntityMetadata metadata();

    /**
     * Metadata for the entity, including namespace, creation time, and last update time.
     * <p>
     * <b> This method will eventually be removed and should be overridden in subclasses. When overriding, the
     * overriding method needs to be annotated with {@code @JsonIgnore(false)}</b>
     *
     * @return Metadata of the entity
     */
    @JsonIgnore
    default MongoEntityMetadata metadata() {
        return MongoEntityMetadata.EMPTY;
    }
}
