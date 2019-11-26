package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.TimeRange;

@AutoValue
@JsonAutoDetect
@JsonIgnoreProperties({ "rangeType", "relative" })
public abstract class FieldChartConfig implements WidgetConfig {
    public abstract String valuetype();
    public abstract String renderer();
    public abstract String interpolation();
    public abstract String field();
    public abstract String interval();

    @JsonCreator
    static FieldChartConfig create(
            @JsonProperty("valuetype") String valuetype,
            @JsonProperty("renderer") String renderer,
            @JsonProperty("interpolation") String interpolation,
            @JsonProperty("field") String field,
            @JsonProperty("interval") String interval,
            @JsonProperty("query") String query,
            @JsonProperty("timerange") TimeRange timerange
        ) {
        return new AutoValue_FieldChartConfig(
                query,
                timerange,
                valuetype,
                renderer,
                interpolation,
                field,
                interval
        );
    }
}
