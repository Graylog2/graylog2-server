package org.graylog.storage.opensearch2.views.searchtypes.pivot;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.Max;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.Min;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EffectiveTimeRangeExtractor {
    private static final Logger LOG = LoggerFactory.getLogger(EffectiveTimeRangeExtractor.class);
    private static final TimeRange ALL_MESSAGES_TIMERANGE = allMessagesTimeRange();
    private static TimeRange allMessagesTimeRange() {
        try {
            return RelativeRange.create(0);
        } catch (InvalidRangeParametersException e) {
            LOG.error("Unable to instantiate all messages timerange: ", e);
        }
        return null;
    }

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
        return ALL_MESSAGES_TIMERANGE.equals(timeRange);
    }
}
