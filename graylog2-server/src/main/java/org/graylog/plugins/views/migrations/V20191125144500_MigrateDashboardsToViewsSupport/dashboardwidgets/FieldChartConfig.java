package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.TimeRange;

@AutoValue
@JsonAutoDetect
public abstract class FieldChartConfig implements WidgetConfig {
    @JsonProperty
    public abstract String valuetype();

    @JsonProperty
    public abstract String renderer();

    @JsonProperty
    public abstract String interpolation();

    @JsonProperty
    public abstract String field();

    @JsonCreator
    static FieldChartConfig create(
            @JsonProperty("valuetype") String valuetype,
            @JsonProperty("renderer") String renderer,
            @JsonProperty("interpolation") String interpolation,
            @JsonProperty("field") String field,
            @JsonProperty("query") String query,
            @JsonProperty("timerange") TimeRange timerange
    ) {
        return new AutoValue_FieldChartConfig(
                query,
                timerange,
                valuetype,
                renderer,
                interpolation,
                field
        );
    }
}
