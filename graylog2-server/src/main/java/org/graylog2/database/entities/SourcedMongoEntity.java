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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.database.BuildableMongoEntity;
import org.graylog2.database.entities.source.EntitySource;

import java.util.Optional;

public interface SourcedMongoEntity<T, B extends SourcedMongoEntity.Builder<T, B>> extends BuildableMongoEntity<T, B> {
    String FIELD_ENTITY_SOURCE = "_entity_source";

    /**
     * The entity source information, if available. This field is populated via the
     * {@link org.graylog2.database.pagination.EntitySourceLookup} aggregation stage from the "entity_source" collection
     * and is not stored directly in any entity's collection. The access and inclusion annotations enforce this behavior.
     */
    @JsonProperty(value = FIELD_ENTITY_SOURCE, access = JsonProperty.Access.READ_ONLY)
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    Optional<EntitySource> entitySource();

    interface Builder<T, B> extends BuildableMongoEntity.Builder<T, B> {

        @JsonProperty(FIELD_ENTITY_SOURCE)
        B entitySource(Optional<EntitySource> source);
    }
}
