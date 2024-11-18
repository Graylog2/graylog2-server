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
package org.graylog.storage.opensearch2.views.searchtypes.pivot;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.Max;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.Min;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class EffectiveTimeRangeExtractor {
    AbsoluteRange extract(SearchResponse queryResult, Query query, Pivot pivot) {
        if (queryResult.getHits().getTotalHits().value != 0) {
            return getAbsoluteRangeFromAggregations(queryResult, query, pivot);
        } else {
            return getAbsoluteRangeFromPivot(query, pivot);
        }
    }
    private AbsoluteRange getAbsoluteRangeFromPivot(final Query query, final Pivot pivot) {
        final TimeRange pivotRange = query.effectiveTimeRange(pivot);
        return AbsoluteRange.create(pivotRange.getFrom(), pivotRange.getTo());
    }

    private AbsoluteRange getAbsoluteRangeFromAggregations(final SearchResponse queryResult, final Query query, final Pivot pivot) {
        final Min min = queryResult.getAggregations().get("timestamp-min");
        final Double from = min.getValue();
        final Max max = queryResult.getAggregations().get("timestamp-max");
        final Double to = max.getValue();
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
