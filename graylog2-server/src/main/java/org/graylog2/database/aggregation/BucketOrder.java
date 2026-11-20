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

import org.graylog2.rest.models.SortOrder;

import java.util.Objects;

/**
 * The order of buckets, which also decides which buckets a limit keeps. Buckets with equal counts are ordered by key,
 * ascending, so the order is stable. Keys of different types follow the BSON comparison order.
 *
 * @param sortBy    whether to order by count or by key
 * @param direction the sort direction
 */
public record BucketOrder(SortBy sortBy, SortOrder direction) {
    public enum SortBy {
        COUNT, KEY
    }

    public BucketOrder {
        Objects.requireNonNull(sortBy, "sortBy");
        Objects.requireNonNull(direction, "direction");
    }

    public static BucketOrder byCount(final SortOrder direction) {
        return new BucketOrder(SortBy.COUNT, direction);
    }

    public static BucketOrder byKey(final SortOrder direction) {
        return new BucketOrder(SortBy.KEY, direction);
    }
}
