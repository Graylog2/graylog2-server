package org.graylog.storage.elasticsearch7.views.searchtypes.pivot;

import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.common.xcontent.XContentBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregations;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;

import java.io.IOException;

public class InitialBucket implements MultiBucketsAggregation.Bucket {
    private final long docCount;
    private final Aggregations aggregations;

    private InitialBucket(long docCount, Aggregations aggregations) {
        this.docCount = docCount;
        this.aggregations = aggregations;
    }

    public static InitialBucket create(SearchResponse searchResponse) {
        return new InitialBucket(searchResponse.getHits().getTotalHits().value, searchResponse.getAggregations());
    }

    @Override
    public Object getKey() {
        throw new IllegalStateException("Not implemented!");
    }

    @Override
    public String getKeyAsString() {
        throw new IllegalStateException("Not implemented!");
    }

    @Override
    public long getDocCount() {
        return this.docCount;
    }

    @Override
    public Aggregations getAggregations() {
        return this.aggregations;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder xContentBuilder, Params params) throws IOException {
        throw new IllegalStateException("Not implemented!");
    }
}
