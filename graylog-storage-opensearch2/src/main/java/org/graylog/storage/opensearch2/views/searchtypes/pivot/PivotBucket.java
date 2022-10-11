package org.graylog.storage.opensearch2.views.searchtypes.pivot;

import com.google.common.collect.ImmutableList;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.MultiBucketsAggregation;

public record PivotBucket(ImmutableList<String> keys, MultiBucketsAggregation.Bucket bucket) {
    public static PivotBucket create(ImmutableList<String> keys, MultiBucketsAggregation.Bucket bucket) {
        return new PivotBucket(keys, bucket);
    }
}
