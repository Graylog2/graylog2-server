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

import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.graylog2.database.filtering.inmemory.InMemoryFilterable;

import java.util.Objects;
import java.util.function.Predicate;

public record SingleValueFilter(String field, Object value) implements Filter {

    @Override
    public Bson toBson() {
        return Filters.eq(field(), value());
    }

    @Override
    public Predicate<InMemoryFilterable> toPredicate() {
        return o -> o.extractFieldValue(field)
                .map(fieldValue -> fieldValue.equals(value))
                .orElse(false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final SingleValueFilter that = (SingleValueFilter) o;
        return Objects.equals(field, that.field) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, value);
    }
}
