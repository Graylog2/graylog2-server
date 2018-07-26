package org.graylog.plugins.enterprise.search.searchtypes.pivot.buckets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

@AutoValue
@JsonTypeName(TimeUnitInterval.type)
public abstract class TimeUnitInterval implements Interval {
    public static final String type = "timeunit";

    @JsonProperty
    public abstract String type();

    @JsonProperty
    public abstract String timeunit();

    @JsonCreator
    public static TimeUnitInterval create(@JsonProperty("timeunit") String timeunit) {
        return new AutoValue_TimeUnitInterval(type, timeunit);
    }

    @Override
    public DateHistogramInterval toDateHistogramInterval(TimeRange timerange) {
        return new DateHistogramInterval(timeunit());
    }
}
