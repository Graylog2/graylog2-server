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
package org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series;

import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregation;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregations;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ValueCount;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ValueCountAggregationBuilder;
import org.graylog.storage.elasticsearch7.views.ESGeneratedQueryContext;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.ESPivot;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.ESPivotSeriesSpecHandler;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public class ESCountHandler extends ESPivotSeriesSpecHandler<Count, ValueCount> {
    private static final Logger LOG = LoggerFactory.getLogger(ESCountHandler.class);

    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, Count count, ESPivot searchTypeHandler, ESGeneratedQueryContext queryContext) {
        final String field = count.field();
        if (field == null) {
            // doc_count is always present in elasticsearch's bucket aggregations, no need to add it
            return Optional.empty();
        } else {
            // the request was for a field count, we have to add a value_count sub aggregation
            final ValueCountAggregationBuilder value = AggregationBuilders.count(name).field(field);
            record(queryContext, pivot, count, name, ValueCount.class);
            return Optional.of(value);
        }
    }

    @Override
    public Stream<Value> doHandleResult(Pivot pivot,
                                        Count count,
                                        SearchResponse searchResult,
                                        ValueCount metricAggregation,
                                        ESPivot searchTypeHandler,
                                        ESGeneratedQueryContext esGeneratedQueryContext) {
        Object value = null;
        if (metricAggregation instanceof MultiBucketsAggregation.Bucket) {
            value = ((MultiBucketsAggregation.Bucket) metricAggregation).getDocCount();
        } else if (metricAggregation instanceof Aggregations) {
            value = searchResult.getHits().getTotalHits().value;
        } else if (metricAggregation != null) {
            value = metricAggregation.getValue();
        }
        if (value == null) {
            LOG.error("Unexpected aggregation type {}, returning 0 for the count. This is a bug.", metricAggregation);
            value = 0;
        }
        return Stream.of(Value.create(count.id(), Count.NAME, value));
    }

    @Override
    protected Aggregation extractAggregationFromResult(Pivot pivot, PivotSpec spec, Aggregations aggregations, ESGeneratedQueryContext queryContext) {
        final Tuple2<String, Class<? extends Aggregation>> objects = aggTypes(queryContext, pivot).getTypes(spec);
        if (objects == null) {
            return null;
        } else {
            // try to saved sub aggregation type. this might fail if we refer to the total result of the entire result instead of a specific
            // value_count aggregation. we'll handle that special case in doHandleResult above
            return aggregations.get(objects.v1);
        }
    }
}
