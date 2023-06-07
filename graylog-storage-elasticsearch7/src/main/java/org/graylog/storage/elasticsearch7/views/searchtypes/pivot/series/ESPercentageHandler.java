package org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series;

import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Percentage;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.xcontent.XContentBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregation;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregations;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.HasAggregations;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.missing.Missing;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.ValueCount;
import org.graylog.storage.elasticsearch7.views.ESGeneratedQueryContext;
import org.graylog.storage.elasticsearch7.views.searchtypes.ESSearchTypeHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.ESPivotSeriesSpecHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.SeriesAggregationBuilder;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ESPercentageHandler extends ESPivotSeriesSpecHandler<Percentage, ValueCount> {
    private static final Logger LOG = LoggerFactory.getLogger(ESCountHandler.class);

    @Nonnull
    @Override
    public List<SeriesAggregationBuilder> doCreateAggregation(String name, Pivot pivot, Percentage percentage, ESSearchTypeHandler<Pivot> searchTypeHandler, ESGeneratedQueryContext queryContext) {
        return List.of();
    }

    @Override
    public Stream<Value> doHandleResult(Pivot pivot,
                                        Percentage percentage,
                                        SearchResponse searchResult,
                                        ValueCount valueCount,
                                        ESSearchTypeHandler<Pivot> searchTypeHandler,
                                        ESGeneratedQueryContext esGeneratedQueryContext) {
        final long value;
        if (valueCount == null) {
            LOG.error("Unexpected null aggregation result, returning 0 for the count. This is a bug.");
            value = 0;
        } else if (valueCount instanceof MultiBucketsAggregation.Bucket) {
            value = ((MultiBucketsAggregation.Bucket) valueCount).getDocCount();
        } else if (valueCount instanceof Aggregations) {
            value = searchResult.getHits().getTotalHits().value;
        } else {
            value = valueCount.getValue();
        }

        var totalCount = searchResult.getHits().getTotalHits().value;
        var bucketPercentage = (double) value / totalCount;
        return Stream.of(Value.create(percentage.id(), Percentage.NAME, bucketPercentage));
    }

    @Override
    protected Aggregation extractAggregationFromResult(Pivot pivot, PivotSpec spec, HasAggregations aggregations, ESGeneratedQueryContext queryContext) {
        final Tuple2<String, Class<? extends Aggregation>> objects = aggTypes(queryContext, pivot).getTypes(spec);
        if (objects == null) {
            if (aggregations instanceof MultiBucketsAggregation.Bucket) {
                return createValueCount(((MultiBucketsAggregation.Bucket) aggregations).getDocCount());
            } else if (aggregations instanceof Missing) {
                return createValueCount(((Missing) aggregations).getDocCount());
            }
        } else {
            // try to saved sub aggregation type. this might fail if we refer to the total result of the entire result instead of a specific
            // value_count aggregation. we'll handle that special case in doHandleResult above
            return aggregations.getAggregations().get(objects.v1);
        }

        return null;
    }

    private Aggregation createValueCount(final Long docCount) {
        return new ValueCount() {
            @Override
            public long getValue() {
                return docCount;
            }

            @Override
            public double value() {
                return docCount;
            }

            @Override
            public String getValueAsString() {
                return docCount.toString();
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public String getType() {
                return null;
            }

            @Override
            public Map<String, Object> getMetadata() {
                return null;
            }

            @Override
            public XContentBuilder toXContent(XContentBuilder xContentBuilder, Params params) throws IOException {
                return null;
            }
        };
    }
}
