package org.graylog.storage.elasticsearch7.views.searchtypes.pivot;

import com.google.common.collect.ImmutableList;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;

public record PivotBucket(ImmutableList<String> keys, MultiBucketsAggregation.Bucket bucket) {
    public static PivotBucket create(ImmutableList<String> keys, MultiBucketsAggregation.Bucket bucket) {
        return new PivotBucket(keys, bucket);
    }
}
