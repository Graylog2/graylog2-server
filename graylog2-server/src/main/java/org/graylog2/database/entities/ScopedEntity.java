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

public abstract class ScopedEntity {
    public static final String FIELD_ID = "id";
    public static final String FIELD_SCOPE = "scope";

    @Id
    @ObjectId
    @Nullable
    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_SCOPE)
    @Nullable
    public abstract String scope();

    public static abstract class Builder<SELF extends Builder<SELF>> {
        @Id
        @ObjectId
        @JsonProperty(FIELD_ID)
        public abstract SELF id(@Nullable String id);

        @JsonProperty(FIELD_SCOPE)
        public abstract SELF scope(String scope);
    }
}
