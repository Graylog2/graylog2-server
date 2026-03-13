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
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.resources.entities.EntityAttribute;

import java.util.List;

/**
 * Resolves a sort field name and order into the correct MongoDB sort BSON,
 * handling attributes that require aggregation pipeline stages for sorting.
 * <p>
 * This is the sorting counterpart to {@link DbQueryCreator} for filtering.
 *
 * @see org.graylog2.rest.resources.entities.AttributeSortSpec
 * @see org.graylog2.search.AttributeFieldSorts
 */
public final class DbSortResolver {

    private DbSortResolver() {}

    /**
     * Resolves how to sort on the given field, consulting the entity attribute definitions
     * for any custom sort specifications.
     *
     * @param attributes the entity attribute definitions
     * @param sortField  the sort field name from the REST API (attribute id)
     * @param order      the sort direction
     * @return a resolved sort containing the BSON sort and any required pipeline stages
     */
    public static ResolvedSort resolve(List<EntityAttribute> attributes,
                                       String sortField,
                                       SortOrder order) {
        final var attr = attributes.stream()
                .filter(a -> a.id().equals(sortField))
                .findFirst();

        if (attr.isPresent() && attr.get().sortSpec() != null) {
            final var spec = attr.get().sortSpec();
            return new ResolvedSort(
                    spec.preSortStages(),
                    order.toBsonSort(spec.sortField()),
                    spec.postSortStages());
        }

        // No sort spec — use dbField if present, otherwise fall back to the field name as-is.
        final var dbField = attr
                .map(a -> a.dbField() != null ? a.dbField() : a.id())
                .orElse(sortField);
        return new ResolvedSort(List.of(), order.toBsonSort(dbField), List.of());
    }

    /**
     * The result of resolving a sort field. Contains the BSON sort expression and any
     * aggregation pipeline stages needed to make the sort work.
     *
     * @param preSortStages  pipeline stages to add before {@code $sort} (empty if not needed)
     * @param sort           the BSON sort expression
     * @param postSortStages pipeline stages to add after {@code $sort} (empty if not needed)
     */
    public record ResolvedSort(List<Bson> preSortStages, Bson sort, List<Bson> postSortStages) {
        /**
         * Returns {@code true} if this sort requires aggregation pipeline stages,
         * meaning a simple {@code find().sort()} is not sufficient.
         */
        public boolean needsPipeline() {
            return !preSortStages.isEmpty() || !postSortStages.isEmpty();
        }
    }
}
