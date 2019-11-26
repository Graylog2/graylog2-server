package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.TimeRange;

import javax.annotation.Nullable;

@AutoValue
@JsonAutoDetect
public abstract class StatsCountConfig implements WidgetConfig {
    public abstract String field();
    public abstract String statsFunction();
    public abstract Boolean lowerIsBetter();
    public abstract Boolean trend();
    @Nullable
    public abstract String streamId();

    @JsonCreator
    static StatsCountConfig create(
            @JsonProperty("field") String field,
            @JsonProperty("stats_function") String statsFunction,
            @JsonProperty("lower_is_better") Boolean lowerIsBetter,
            @JsonProperty("trend") Boolean trend,
            @JsonProperty("stream_id") @Nullable String streamId,
            @JsonProperty("query") String query,
            @JsonProperty("timerange") TimeRange timerange
    ) {
        return new AutoValue_StatsCountConfig(
                query,
                timerange,
                field,
                statsFunction,
                lowerIsBetter,
                trend,
                streamId
        );
    }
}
