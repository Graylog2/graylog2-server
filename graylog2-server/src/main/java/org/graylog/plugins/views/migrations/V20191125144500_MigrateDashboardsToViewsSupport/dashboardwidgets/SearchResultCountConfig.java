package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.TimeRange;

@AutoValue
@JsonAutoDetect
public abstract class SearchResultCountConfig implements WidgetConfig {
    public abstract Boolean lowerIsBetter();
    public abstract Boolean trend();

    @JsonCreator
    static SearchResultCountConfig create(
            @JsonProperty("lower_is_better") Boolean lowerIsBetter,
            @JsonProperty("trend") Boolean trend,
            @JsonProperty("query") String query,
            @JsonProperty("timerange") TimeRange timerange
    ) {
        return new AutoValue_SearchResultCountConfig(
                query,
                timerange,
                lowerIsBetter,
                trend
        );
    }
}
