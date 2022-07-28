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
package org.graylog2.database.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

/**
 * Entity base class, which can be used to enforce that each entity implementation
 * has the required id and metadata fields.
 */
// TODO: we might consider renaming these 'Entity' classes.  There is already the notion of 'Entities' in content pack management, which has an entirely different meaning.
public abstract class Entity {
    private static final String ID = "id";
    static final String METADATA = "_metadata";

    // The @Id and @ObjectId annotations take care of converting the "id" field name to "_id" when storing/loading
    // values to/from the database.
    @Id
    @ObjectId
    @Nullable
    @JsonProperty(ID)
    public abstract String id();

    @JsonProperty(METADATA)
    public abstract EntityMetadata metadata();

    public abstract <T extends Entity> T withMetadata(EntityMetadata metadata);

    public static abstract class Builder<SELF extends Builder<SELF>> {
        @Id
        @ObjectId
        @JsonProperty(ID)
        public abstract SELF id(@Nullable String id);

        @JsonProperty(METADATA)
        public abstract SELF metadata(EntityMetadata metadata);
    }
}
