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
package org.graylog.storage.elasticsearch7;

import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.BoolQueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilders;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.Collection;
import java.util.Objects;

public class BoolQueryTools {

    public enum Mode {
        FILTER, MUST
    }

    public static void addTimeRange(BoolQueryBuilder boolQueryBuilder, final TimeRange timeRange, final String identifier, final Mode mode) {
        if (mode == Mode.MUST) {
            boolQueryBuilder.must(
                    Objects.requireNonNull(
                            TimeRangeQueryFactory.create(timeRange),
                            "Timerange for " + identifier + " cannot be found."
                    )
            );
        } else {
            boolQueryBuilder.filter(
                    Objects.requireNonNull(
                            TimeRangeQueryFactory.create(timeRange),
                            "Timerange for " + identifier + " cannot be found."
                    )
            );
        }
    }

    public static void addStreams(BoolQueryBuilder boolQueryBuilder, final Collection<String> streamIds, final Mode mode) {
        if (mode == Mode.MUST) {
            boolQueryBuilder.must(QueryBuilders.termsQuery(Message.FIELD_STREAMS, streamIds));
        } else {
            boolQueryBuilder.filter(QueryBuilders.termsQuery(Message.FIELD_STREAMS, streamIds));
        }
    }
}
