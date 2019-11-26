package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.TimeRange;

@AutoValue
@JsonAutoDetect
public abstract class SearchResultChartConfig implements WidgetConfig {
    @JsonProperty
    public abstract String interval();

    @JsonCreator
    static SearchResultChartConfig create(
            @JsonProperty("interval") String interval,
            @JsonProperty("query") String query,
            @JsonProperty("timerange") TimeRange timerange
    ) {
        return new AutoValue_SearchResultChartConfig(
                query,
                timerange,
                interval
        );
    }
}
