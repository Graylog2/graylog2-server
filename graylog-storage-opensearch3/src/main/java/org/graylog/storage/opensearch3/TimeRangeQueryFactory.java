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
package org.graylog.storage.opensearch3;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.query_dsl.QueryBuilders;
import org.opensearch.client.opensearch._types.query_dsl.RangeQuery;

import javax.annotation.Nullable;

public class TimeRangeQueryFactory {

    @Deprecated
    @Nullable
    public static org.graylog.shaded.opensearch2.org.opensearch.index.query.RangeQueryBuilder create(TimeRange range) {
        if (range == null) {
            return null;
        }

        return org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilders.rangeQuery(Message.FIELD_TIMESTAMP)
                .gte(Tools.buildElasticSearchTimeFormat(range.getFrom()))
                .lt(Tools.buildElasticSearchTimeFormat(range.getTo()));
    }

    @Nullable
    public static RangeQuery createTimeRangeQuery(TimeRange range) {
        if (range == null) {
            return null;
        }
        return QueryBuilders.range()
                .field(Message.FIELD_TIMESTAMP)
                .gte(JsonData.of(Tools.buildElasticSearchTimeFormat(range.getFrom())))
                .lt(JsonData.of(Tools.buildElasticSearchTimeFormat(range.getTo())))
                .build();
    }
}
