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
package org.graylog2.database.slicing;

import org.graylog2.database.filtering.DbQueryCreator;
import org.graylog2.rest.resources.entities.EntityAttribute;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An entity list that can be sliced: where its entities are stored, its columns, and how its query and filters
 * become a MongoDB filter.
 *
 * @param collection   the collection holding the entities
 * @param attributes   the list's columns; those marked {@link EntityAttribute#sliceable() sliceable} can be sliced
 * @param queryCreator the query creator the list itself uses, so slice counts match what the list shows
 */
public record SliceableEntity(String collection, List<EntityAttribute> attributes, DbQueryCreator queryCreator) {
    public SliceableEntity {
        Objects.requireNonNull(collection, "collection");
        attributes = List.copyOf(attributes);
        Objects.requireNonNull(queryCreator, "queryCreator");
    }

    public Optional<EntityAttribute> sliceableAttribute(final String id) {
        return attributes.stream()
                .filter(attribute -> attribute.id().equals(id) && Boolean.TRUE.equals(attribute.sliceable()))
                .findFirst();
    }

    public List<String> sliceableAttributeIds() {
        return attributes.stream()
                .filter(attribute -> Boolean.TRUE.equals(attribute.sliceable()))
                .map(EntityAttribute::id)
                .toList();
    }
}
