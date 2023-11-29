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
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.graylog2.database.filtering.inmemory.InMemoryFilterable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public record RangeFilter(String field, Object from, Object to) implements Filter {

    @Override
    public Bson toBson() {
        List<Bson> rangeFilters = new ArrayList<>(2);
        if (from() != null) {
            rangeFilters.add(Filters.gte(field(), from()));
        }
        if (to() != null) {
            rangeFilters.add(Filters.lte(field(), to()));
        }
        if (!rangeFilters.isEmpty()) {
            return Filters.and(rangeFilters);
        } else {
            return new BsonDocument();
        }
    }

    @Override
    public Predicate<InMemoryFilterable> toPredicate() {
        //TODO: we do not have a use case for that, but maybe this one needs to be implemented for future use cases...
        throw new UnsupportedOperationException("RangeFilters are only supported in MongoDB-based filtering, not in in-memory filtering.");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final RangeFilter that = (RangeFilter) o;
        return Objects.equals(field, that.field) && Objects.equals(from, that.from) && Objects.equals(to, that.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, from, to);
    }
}
