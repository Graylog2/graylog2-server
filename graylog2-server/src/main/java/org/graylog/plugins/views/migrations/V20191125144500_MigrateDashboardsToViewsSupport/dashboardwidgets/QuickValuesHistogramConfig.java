package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.TimeRange;

@AutoValue
@JsonAutoDetect
public abstract class QuickValuesHistogramConfig implements WidgetConfig {
    public abstract String field();
    public abstract Integer limit();
    public abstract String sortOrder();
    public abstract String stackedFields();

    @JsonCreator
    static QuickValuesHistogramConfig create(
            @JsonProperty("query") String query,
            @JsonProperty("timerange") TimeRange timerange,
            @JsonProperty("field") String field,
            @JsonProperty("limit") Integer limit,
            @JsonProperty("sort_order") String sortOrder,
            @JsonProperty("stacked_fields") String stackedFields
    ) {
        return new AutoValue_QuickValuesHistogramConfig(
                query,
                timerange,
                field,
                limit,
                sortOrder,
                stackedFields
        );
    }
}
