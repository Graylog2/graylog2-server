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
package org.graylog2.rest.resources.entities;

import org.bson.conversions.Bson;

import java.util.List;

/**
 * Describes how to sort on an entity attribute that doesn't map to a simple MongoDB field.
 * <p>
 * For attributes stored in nested structures (e.g., key-value arrays) or computed from other
 * fields, sorting requires aggregation pipeline stages to extract/compute a sortable value
 * into a temporary field.
 *
 * @param preSortStages  pipeline stages to add before {@code $sort} (e.g., {@code $addFields} to extract a value)
 * @param sortField      the MongoDB field name to sort on (may be a temporary field created by preSortStages)
 * @param postSortStages pipeline stages to add after {@code $sort} (e.g., {@code $unset} to clean up temporary fields)
 * @see AttributeFieldSorts
 */
public record AttributeSortSpec(
        List<Bson> preSortStages,
        String sortField,
        List<Bson> postSortStages
) {
    /**
     * Creates a sort spec for an attribute that maps directly to a different MongoDB field.
     * No pipeline stages are needed — only the field name is remapped.
     * <p>
     * Example: a "status" attribute that sorts on the underlying "last_seen" field.
     *
     * @param dbField the actual MongoDB field name to sort on
     */
    public static AttributeSortSpec field(String dbField) {
        return new AttributeSortSpec(List.of(), dbField, List.of());
    }

    /**
     * Returns {@code true} if this sort spec requires aggregation pipeline stages,
     * meaning a simple {@code find().sort()} is not sufficient.
     */
    public boolean needsPipeline() {
        return !preSortStages.isEmpty() || !postSortStages.isEmpty();
    }
}
