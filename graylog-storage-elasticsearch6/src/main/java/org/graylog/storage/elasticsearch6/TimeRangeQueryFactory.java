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
package org.graylog.storage.elasticsearch6;

import org.graylog.shaded.elasticsearch5.org.elasticsearch.index.query.QueryBuilders;
import org.graylog.shaded.elasticsearch5.org.elasticsearch.index.query.RangeQueryBuilder;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;

public class TimeRangeQueryFactory {
    @Nullable
    public static RangeQueryBuilder create(TimeRange range) {
        if (range == null) {
            return null;
        }

        return QueryBuilders.rangeQuery(Message.FIELD_TIMESTAMP)
                .gte(Tools.buildElasticSearchTimeFormat(range.getFrom()))
                .lte(Tools.buildElasticSearchTimeFormat(range.getTo()));
    }
}
