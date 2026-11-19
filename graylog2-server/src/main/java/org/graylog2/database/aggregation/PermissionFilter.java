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
package org.graylog2.database.aggregation;

import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Objects;

/**
 * The documents a user may read, as a MongoDB filter, e.g. the events of the streams they have access to.
 * <p>
 * Counting is only correct if the documents the user can't read are left out before grouping, so aggregating for a
 * user requires a permission filter instead of accepting any filter. A caller that doesn't restrict access says so with
 * {@link #unrestricted()}.
 *
 * @see CatalogPermissionFilters
 * @see PermittedAggregator
 *
 * @param filter matches the documents the user may read; should be backed by an index
 */
public record PermissionFilter(Bson filter) {
    public PermissionFilter {
        Objects.requireNonNull(filter, "filter");
    }

    public static PermissionFilter of(final Bson filter) {
        return new PermissionFilter(filter);
    }

    public static PermissionFilter unrestricted() {
        return new PermissionFilter(new Document());
    }
}
