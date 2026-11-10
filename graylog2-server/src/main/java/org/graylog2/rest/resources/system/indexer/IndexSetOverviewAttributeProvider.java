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
package org.graylog2.rest.resources.system.indexer;

import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.rest.resources.entities.EntityAttribute;

import java.util.List;
import java.util.Map;

/**
 * Lets a plugin add attributes to the paginated index sets list, {@code GET /system/indices/index_sets/paginated}.
 * <p>
 * The attributes are appended to the built-in ones of {@link IndexSetsResource} for every page request, so the
 * table in the web interface sees them as columns and can sort and filter by them. An attribute carries its own
 * {@link org.graylog2.rest.resources.entities.AttributeSortSpec} and filter: a sort has to be worked out in
 * MongoDB, a filter either maps to stored fields or builds its own query through
 * {@link EntityAttribute#bsonFilterCreator()}. The value a cell shows comes from {@link PageAttributes#values}.
 * <p>
 * Implementations are added to the {@code Set<IndexSetOverviewAttributeProvider>} multibinding.
 */
public interface IndexSetOverviewAttributeProvider {

    /**
     * The attributes of this provider for one page request. Called once per request, before anything else of the
     * request is worked out, so an implementation that depends on state that can change at runtime, such as a
     * global setting, reads that state once here and builds sort, filter and values from that one reading. The
     * three then cannot disagree within a page.
     */
    PageAttributes forPage();

    /**
     * The attributes a provider contributes to one page, and their values for the index sets of that page.
     * <p>
     * Attribute ids must not collide with the built-in attributes, see {@link IndexSetsResource#builtInAttributes()},
     * nor with the fields of a row itself, such as {@code title}: a value is written as a top-level field of its row
     * under the attribute id, and a repeated key would corrupt the JSON. A test of the plugin should check both.
     */
    interface PageAttributes {

        List<EntityAttribute> attributes();

        /**
         * The values of this provider's attributes for the index sets of the page, keyed by index set id and then
         * by attribute id. Each value is written as a top-level field of its row, named after the attribute, which
         * is where the table reads a column's value from. A value may be {@code null}, so the inner map has to
         * allow it. The default provides no values.
         */
        default Map<String, Map<String, Object>> values(List<IndexSetConfig> indexSets) {
            return Map.of();
        }
    }
}
