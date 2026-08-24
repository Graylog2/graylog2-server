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
package org.graylog.storage.opensearch3.views.searchtypes.pivot;

import jakarta.annotation.Nonnull;
import org.opensearch.client.opensearch._types.aggregations.MultiBucketBase;

/**
 * Stands in for the terms that fell outside a values grouping's limit. There is no such bucket in the search
 * response, so its series values are derived and carried on {@link PivotBucket#derivedValues()} instead. Nothing
 * should ever walk into this bucket's aggregations: it is always a leaf.
 */
public class OtherBucket extends MultiBucketBase {

    private OtherBucket(Builder b) {
        super(b);
    }

    public static OtherBucket create(long docCount) {
        return new Builder().docCount(docCount).build();
    }

    public static class Builder extends MultiBucketBase.AbstractBuilder<Builder> {
        @Override
        @Nonnull
        protected Builder self() {
            return this;
        }

        @Nonnull
        public OtherBucket build() {
            return new OtherBucket(this);
        }
    }
}
