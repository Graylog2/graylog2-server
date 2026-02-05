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
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.aggregations.MultiBucketBase;
import org.opensearch.client.opensearch.core.msearch.MultiSearchItem;
import org.opensearch.client.opensearch.core.search.TotalHits;

public class InitialBucket extends MultiBucketBase {

    private InitialBucket(Builder b) {
        super(b);
    }

    @Nonnull
    public Builder toBuilder() {
        return new Builder(this);
    }

    @Nonnull
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends MultiBucketBase.AbstractBuilder<Builder> {
        public Builder() {
        }

        private Builder(InitialBucket b) {
            super(b);
        }

        private Builder(Builder b) {
            super(b);
        }

        @Override
        @Nonnull
        protected Builder self() {
            return this;
        }

        @Nonnull
        public InitialBucket build() {
            return new InitialBucket(this);
        }

    }

    public static InitialBucket create(MultiSearchItem<JsonData> searchResponse) {
        TotalHits total = searchResponse.hits().total();
        long value = (total != null) ? total.value() : 0;
        return InitialBucket.builder().docCount(value).aggregations(searchResponse.aggregations()).build();
    }

}
