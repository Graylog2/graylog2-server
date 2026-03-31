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

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import org.bson.Document;
import org.graylog2.rest.resources.entities.AttributeSortSpec;

import java.util.List;

/**
 * Provides reusable {@link AttributeSortSpec} factory methods for sorting on
 * non-trivial MongoDB field structures.
 * <p>
 * This is the sorting counterpart to {@link AttributeFieldFilters}.
 *
 * @see AttributeFieldFilters
 */
public final class AttributeFieldSorts {

    private AttributeFieldSorts() {}

    /**
     * Creates a sort spec that extracts a value from a key-value attribute array
     * into a temporary field for sorting.
     * <p>
     * Given an array like {@code [{key: "host.name", value: "server01"}, ...]},
     * this generates pipeline stages equivalent to:
     * <pre>{@code
     * {$set: {"_sort_host_name": {$let: {
     *     vars: {match: {$arrayElemAt: [{$filter: {
     *         input: "$non_identifying_attributes",
     *         cond: {$eq: ["$$this.key", "host.name"]}
     *     }}, 0]}},
     *     in: "$$match.value"
     * }}}}
     * // ... $sort on _sort_host_name ...
     * {$unset: "_sort_host_name"}
     * }</pre>
     *
     * @param arrayField   the MongoDB field containing the attribute array (e.g., "non_identifying_attributes")
     * @param attributeKey the key to match within the array (e.g., "host.name", "service.version")
     * @return a sort spec with pipeline stages to extract and sort on the attribute value
     */
    public static AttributeSortSpec attributeArray(String arrayField, String attributeKey) {
        final var tempField = "_sort_" + attributeKey.replace(".", "_");
        return new AttributeSortSpec(
                List.of(Aggregates.set(new Field<>(tempField,
                        new Document("$let", new Document("vars",
                                new Document("match", new Document("$arrayElemAt", List.of(
                                        new Document("$filter",
                                                new Document("input", "$" + arrayField)
                                                        .append("cond", new Document("$eq",
                                                                List.of("$$this.key", attributeKey)))),
                                        0))))
                                .append("in", "$$match.value"))))),
                tempField,
                List.of(Aggregates.unset(tempField))
        );
    }
}
