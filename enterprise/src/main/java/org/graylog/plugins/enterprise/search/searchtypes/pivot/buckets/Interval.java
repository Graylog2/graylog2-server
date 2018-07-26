package org.graylog.plugins.enterprise.search.searchtypes.pivot.buckets;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = Interval.TYPE_FIELD,
        visible = true)
public interface Interval {
    String TYPE_FIELD = "type";

    String type();

    DateHistogramInterval toDateHistogramInterval(TimeRange timerange);
}
