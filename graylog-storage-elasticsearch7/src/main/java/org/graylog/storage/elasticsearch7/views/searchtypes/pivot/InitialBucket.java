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
package org.graylog.storage.elasticsearch7.views.searchtypes.pivot;

import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.xcontent.XContentBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregations;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;

import java.io.IOException;

public class InitialBucket implements MultiBucketsAggregation.Bucket {
    private final long docCount;
    private final Aggregations aggregations;

    private InitialBucket(long docCount, Aggregations aggregations) {
        this.docCount = docCount;
        this.aggregations = aggregations;
    }

    public static InitialBucket create(SearchResponse searchResponse) {
        return new InitialBucket(searchResponse.getHits().getTotalHits().value, searchResponse.getAggregations());
    }

    @Override
    public Object getKey() {
        throw new IllegalStateException("Not implemented!");
    }

    @Override
    public String getKeyAsString() {
        throw new IllegalStateException("Not implemented!");
    }

    @Override
    public long getDocCount() {
        return this.docCount;
    }

    @Override
    public Aggregations getAggregations() {
        return this.aggregations;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder xContentBuilder, Params params) throws IOException {
        throw new IllegalStateException("Not implemented!");
    }
}
