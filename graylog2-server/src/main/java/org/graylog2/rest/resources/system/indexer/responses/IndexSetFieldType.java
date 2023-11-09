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
package org.graylog2.rest.resources.system.indexer.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.EntityDefaults;
import org.graylog2.rest.resources.entities.Sorting;

import java.util.Comparator;
import java.util.List;

public record IndexSetFieldType(@JsonProperty(FIELD_NAME) String fieldName,
                                @JsonProperty(TYPE) String type,
                                @JsonProperty(IS_CUSTOM) boolean isCustom,
                                @JsonProperty(IS_RESERVED) boolean isReserved) {

    public static final String FIELD_NAME = "field_name";
    public static final String TYPE = "type";
    public static final String IS_CUSTOM = "is_custom";
    public static final String IS_RESERVED = "is_reserved";

    public static final String DEFAULT_SORT_FIELD = IndexSetFieldType.FIELD_NAME;
    public static final Sorting DEFAULT_SORT = Sorting.create(DEFAULT_SORT_FIELD, Sorting.Direction.ASC);
    public static final EntityDefaults ENTITY_DEFAULTS = EntityDefaults.builder()
            .sort(DEFAULT_SORT)
            .build();

    public static final List<EntityAttribute> ATTRIBUTES = List.of(
            EntityAttribute.builder().id(IndexSetFieldType.FIELD_NAME).title("Field name").sortable(true).build(),
            EntityAttribute.builder().id(IndexSetFieldType.IS_CUSTOM).title("Is Custom").sortable(true).build(),
            EntityAttribute.builder().id(IndexSetFieldType.IS_RESERVED).title("Is Reserved").sortable(true).build(),
            EntityAttribute.builder().id(IndexSetFieldType.TYPE).title("Type").sortable(true).build()
    );

    public static Comparator<IndexSetFieldType> getComparator(final String sort,
                                                              final Sorting.Direction order) {
        final Comparator<IndexSetFieldType> comparator = switch (sort) {
            case TYPE -> Comparator.comparing(IndexSetFieldType::type);
            case IS_RESERVED -> Comparator.comparing(IndexSetFieldType::isReserved);
            case IS_CUSTOM -> Comparator.comparing(IndexSetFieldType::isCustom);
            default -> Comparator.comparing(IndexSetFieldType::fieldName);
        };

        if (order == Sorting.Direction.DESC) {
            return comparator.reversed();
        } else {
            return comparator;
        }
    }
}
