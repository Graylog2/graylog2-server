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
import org.graylog2.database.filtering.inmemory.InMemoryFilterable;
import org.graylog2.indexer.indexset.CustomFieldMappings;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.EntityDefaults;
import org.graylog2.rest.resources.entities.FilterOption;
import org.graylog2.rest.resources.entities.Sorting;
import org.graylog2.search.SearchQueryField;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public record IndexSetFieldType(@JsonProperty(FIELD_NAME) String fieldName,
                                @JsonProperty(TYPE) String type,
                                @JsonProperty(ORIGIN) FieldTypeOrigin origin,
                                @JsonProperty(IS_RESERVED) boolean isReserved) implements InMemoryFilterable {

    static final String FIELD_NAME = "field_name";
    static final String TYPE = "type";
    static final String ORIGIN = "origin";
    static final String IS_RESERVED = "is_reserved";

    public static final String DEFAULT_SORT_FIELD = IndexSetFieldType.FIELD_NAME;
    public static final Sorting DEFAULT_SORT = Sorting.create(DEFAULT_SORT_FIELD, Sorting.Direction.ASC);
    public static final EntityDefaults ENTITY_DEFAULTS = EntityDefaults.builder()
            .sort(DEFAULT_SORT)
            .build();

    public static final List<EntityAttribute> ATTRIBUTES = List.of(
            EntityAttribute.builder().id(IndexSetFieldType.FIELD_NAME).title("Field name")
                    .sortable(true)
                    .filterable(true)
                    .build(),
            EntityAttribute.builder().id(IndexSetFieldType.ORIGIN).title("Origin")
                    .type(SearchQueryField.Type.STRING)
                    .sortable(true)
                    .filterable(true)
                    .filterOptions(
                            Arrays.stream(FieldTypeOrigin.values())
                                    .map(origin -> FilterOption.create(origin.toString(), origin.title()))
                                    .collect(Collectors.toSet())
                    )
                    .build(),
            EntityAttribute.builder().id(IndexSetFieldType.IS_RESERVED).title("Is Reserved")
                    .type(SearchQueryField.Type.BOOLEAN)
                    .sortable(true)
                    .filterable(true)
                    .filterOptions(
                            Set.of(
                                    FilterOption.create("true", "yes"),
                                    FilterOption.create("false", "no")
                            )
                    )
                    .build(),
            EntityAttribute.builder().id(IndexSetFieldType.TYPE).title("Type")
                    .sortable(true)
                    .filterable(true)
                    .filterOptions(CustomFieldMappings.AVAILABLE_TYPES.entrySet().stream()
                            .map(entry -> FilterOption.create(entry.getKey(), entry.getValue().description()))
                            .collect(Collectors.toSet())
                    )
                    .build()
    );

    @Override
    public Optional<Object> extractFieldValue(final String fieldName) {
        return switch (fieldName) {
            case FIELD_NAME -> Optional.ofNullable(fieldName());
            case TYPE -> Optional.ofNullable(type());
            case ORIGIN -> Optional.of(origin()).map(o -> o.toString());
            case IS_RESERVED -> Optional.of(isReserved());
            default -> Optional.empty();
        };
    }

    public static Comparator<IndexSetFieldType> getComparator(final String sort,
                                                              final Sorting.Direction order) {
        final Comparator<IndexSetFieldType> comparator = switch (sort) {
            case TYPE -> Comparator.comparing(IndexSetFieldType::type);
            case IS_RESERVED -> Comparator.comparing(IndexSetFieldType::isReserved);
            case ORIGIN -> Comparator.comparing(IndexSetFieldType::origin);
            default -> Comparator.comparing(IndexSetFieldType::fieldName);
        };

        if (order == Sorting.Direction.DESC) {
            return comparator.reversed();
        } else {
            return comparator;
        }
    }
}
