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
package org.graylog2.database.filtering;

import org.bson.conversions.Bson;
import org.graylog2.database.filtering.inmemory.InMemoryFilterable;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryOperators;
import org.graylog2.search.SearchQueryParser;

import java.util.function.Predicate;

/**
 * A {@link Filter} implementation that delegates BSON query generation to a custom
 * {@link SearchQueryField.BsonFilterCreator}. This is used for non-standard field
 * structures (e.g., attribute arrays stored as key/value pairs) where the default
 * {@code Filters.eq(field, value)} approach doesn't work.
 */
public record BsonFilterCreatorFilter(
        String field,
        SearchQueryField.BsonFilterCreator creator,
        SearchQueryField.Type fieldType,
        Object value
) implements Filter {

    @Override
    public Bson toBson() {
        var fieldValue = new SearchQueryParser.FieldValue(value, SearchQueryOperators.EQUALS, false, null);
        return creator.createFilter(field, fieldValue);
    }

    @Override
    public Predicate<InMemoryFilterable> toPredicate() {
        // In-memory filtering not supported for custom BSON filter creators
        return o -> false;
    }
}
