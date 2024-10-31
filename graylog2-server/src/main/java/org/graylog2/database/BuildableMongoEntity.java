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
import org.mongojack.Id;

/**
 * Interface that ensures that an entity can be converted to a Builder that allows setting the ID on it.
 *
 * @param <T> Type of the entity that provides a #toBuilder() method
 * @param <B> Type of the builder that allows setting the ID
 */
public interface BuildableMongoEntity<T, B extends BuildableMongoEntity.Builder<T, B>> extends MongoEntity {
    B toBuilder();

    interface Builder<T, B> {
        @Id
        @JsonProperty("id")
        B id(String id);

        T build();
    }
}
