package org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.savedsearch;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view.AbsoluteRange;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view.TimeRange;
import org.joda.time.DateTime;

@AutoValue
@JsonAutoDetect
public abstract class AbsoluteTimeRangeQuery implements Query {
    public static final String type = "absolute";

    public abstract String rangeType();
    public abstract String fields();
    public abstract String query();

    public abstract DateTime from();
    public abstract DateTime to();

    @Override
    public TimeRange toTimeRange() {
        return AbsoluteRange.create(from(), to());
    }

    @JsonCreator
    static AbsoluteTimeRangeQuery create(
            @JsonProperty("rangeType") String rangeType,
            @JsonProperty("fields") String fields,
            @JsonProperty("query") String query,
            @JsonProperty("from") DateTime from,
            @JsonProperty("to") DateTime to
    ) {
        return new AutoValue_AbsoluteTimeRangeQuery(rangeType, fields, query, from, to);
    }
}
