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

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.aggregations.MaxAggregate;
import org.opensearch.client.opensearch._types.aggregations.MinAggregate;
import org.opensearch.client.opensearch.core.msearch.MultiSearchItem;

public class EffectiveTimeRangeExtractor {
    AbsoluteRange extract(MultiSearchItem<JsonData> queryResult, Query query, Pivot pivot) {
        if (queryResult.hits().total().value() != 0) {
            return getAbsoluteRangeFromAggregations(queryResult, query, pivot);
        } else {
            return getAbsoluteRangeFromPivot(query, pivot);
        }
    }
    private AbsoluteRange getAbsoluteRangeFromPivot(final Query query, final Pivot pivot) {
        final TimeRange pivotRange = query.effectiveTimeRange(pivot);
        return AbsoluteRange.create(pivotRange.getFrom(), pivotRange.getTo());
    }

    private AbsoluteRange getAbsoluteRangeFromAggregations(final MultiSearchItem<JsonData> queryResult, final Query query, final Pivot pivot) {
        final MinAggregate min = queryResult.aggregations().get("timestamp-min").min();
        final Double from = min.value();
        final MaxAggregate max = queryResult.aggregations().get("timestamp-max").max();
        final Double to = max.value();
        final TimeRange pivotRange = query.effectiveTimeRange(pivot);
        return AbsoluteRange.create(
                isAllMessagesTimeRange(pivotRange) && from != 0
                        ? new DateTime(from.longValue(), DateTimeZone.UTC)
                        : pivotRange.getFrom(),
                isAllMessagesTimeRange(pivotRange) && to != 0
                        ? new DateTime(to.longValue(), DateTimeZone.UTC)
                        : pivotRange.getTo()
        );
    }
    private boolean isAllMessagesTimeRange(TimeRange timeRange) {
        return timeRange instanceof RelativeRange relativeRange && relativeRange.isAllMessages();
    }
}
