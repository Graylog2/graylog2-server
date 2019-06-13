/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.views.search.elasticsearch.searchtypes;

import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.Bucket;
import io.searchbox.core.search.aggregation.DateHistogramAggregation;
import io.searchbox.core.search.aggregation.MetricAggregation;
import one.util.streamex.StreamEx;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog.plugins.views.search.searchtypes.DateHistogram;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ESGeneratedQueryContext;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;

import java.util.Map;

public class ESDateHistogram implements ESSearchTypeHandler<DateHistogram> {
    @Override
    public void doGenerateQueryPart(SearchJob job, Query query, DateHistogram dateHistogram, ESGeneratedQueryContext queryContext) {
        AggregationBuilder builder = AggregationBuilders.dateHistogram(dateHistogram.id())
                .field(Message.FIELD_TIMESTAMP)
                .dateHistogramInterval(dateHistogram.interval().toESInterval());
        queryContext.addAggregation(builder, dateHistogram);
    }

    @SuppressWarnings("unchecked")
    @Override
    public SearchType.Result doExtractResult(SearchJob job, Query query, DateHistogram searchType, SearchResult result, MetricAggregation aggregations, ESGeneratedQueryContext queryContext) {
        final DateHistogramAggregation dateHistogramAggregation = aggregations.getDateHistogramAggregation(searchType.id());
        final Map<Long, Long> buckets = StreamEx.of(dateHistogramAggregation.getBuckets())
                .mapToEntry(bucket -> new DateTime(bucket.getKey()).getMillis() / 1000L,
                        Bucket::getCount)
                .toMap();

        final TimeRange timerange = query.timerange();
        return DateHistogram.Result.result(searchType.id())
                .results(buckets)
                .timerange(AbsoluteRange.create(timerange.getFrom(), timerange.getTo()))
                .build();

    }
}
