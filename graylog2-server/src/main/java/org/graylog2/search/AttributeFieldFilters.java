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
package org.graylog2.search;

import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;

/**
 * Provides reusable {@link SearchQueryField.BsonFilterCreator} implementations for
 * querying MongoDB documents with key-value attribute arrays.
 * <p>
 * Attribute arrays are stored as {@code [{key: "attr.name", value: "attr.value"}, ...]}.
 * Standard field-path queries don't work for these — instead we need {@code $elemMatch}
 * to match both key and value within the same array element.
 */
public final class AttributeFieldFilters {

    private AttributeFieldFilters() {}

    /**
     * Creates a {@link SearchQueryField.BsonFilterCreator} that generates {@code $elemMatch}
     * queries for key-value attribute arrays.
     * <p>
     * The generated BSON looks like:
     * <pre>{@code
     * { "arrayField": { "$elemMatch": { "key": attributeKey, "value": <operator match> } } }
     * }</pre>
     *
     * @param attributeKey the key to match within the array (e.g., "host.name", "os.type")
     * @return a BsonFilterCreator that produces $elemMatch queries
     */
    public static SearchQueryField.BsonFilterCreator attributeArray(String attributeKey) {
        return (fieldName, fieldValue) -> {
            final Bson valueMatcher = fieldValue.getOperator().buildBson("value", fieldValue.getValue());
            return Filters.elemMatch(fieldName,
                    Filters.and(Filters.eq("key", attributeKey), valueMatcher));
        };
    }
}
