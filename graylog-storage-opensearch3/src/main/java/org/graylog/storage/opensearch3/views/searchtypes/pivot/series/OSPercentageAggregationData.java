package org.graylog.storage.opensearch3.views.searchtypes.pivot.series;

import org.graylog.shaded.opensearch2.org.opensearch.core.xcontent.XContentBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregation;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.ValueCount;

import java.io.IOException;
import java.util.Map;

public class OSPercentageAggregationData implements Aggregation {

    private final MultiBucketsAggregation.Bucket initialBucket;
    private final ValueCount valueCount;

    public OSPercentageAggregationData(final ValueCount valueCount,
                                       final MultiBucketsAggregation.Bucket initialBucket) {
        this.valueCount = valueCount;
        this.initialBucket = initialBucket;
    }

    public MultiBucketsAggregation.Bucket initialBucket() {
        return initialBucket;
    }

    public ValueCount valueCount() {
        return valueCount;
    }

    @Override
    public String getName() {
        return valueCount.getName();
    }

    @Override
    public String getType() {
        return valueCount.getType();
    }

    @Override
    public Map<String, Object> getMetadata() {
        return valueCount.getMetadata();
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        return valueCount.toXContent(builder, params);
    }
}
