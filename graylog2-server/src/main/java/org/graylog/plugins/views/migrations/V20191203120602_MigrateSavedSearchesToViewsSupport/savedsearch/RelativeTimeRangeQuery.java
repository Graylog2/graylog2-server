package org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.savedsearch;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view.RelativeRange;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.view.TimeRange;

@AutoValue
@JsonAutoDetect
public abstract class RelativeTimeRangeQuery implements Query {
    public static final String type = "relative";

    public abstract String rangeType();
    public abstract String fields();
    public abstract String query();
    public abstract int relative();

    @Override
    public TimeRange toTimeRange() {
        return RelativeRange.create(relative());
    }

    @JsonCreator
    static RelativeTimeRangeQuery create(
            @JsonProperty("rangeType") String rangeType,
            @JsonProperty("fields") String fields,
            @JsonProperty("query") String query,
            @JsonProperty("relative") int relative
    ) {
        return new AutoValue_RelativeTimeRangeQuery(rangeType, fields, query, relative);
    }
}
