package org.graylog.plugins.views.search.searchtypes.pivot.buckets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = Interval.TYPE_FIELD,
        defaultImpl = TimeUnitInterval.class)
@JsonAutoDetect
public interface Interval {
    String TYPE_FIELD = "type";

    @JsonProperty(TYPE_FIELD)
    String type();

    DateHistogramInterval toDateHistogramInterval(TimeRange timerange);
}
