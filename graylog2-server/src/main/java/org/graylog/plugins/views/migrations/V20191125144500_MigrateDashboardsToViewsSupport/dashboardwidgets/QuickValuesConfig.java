package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.TimeRange;

@AutoValue
@JsonAutoDetect
public abstract class QuickValuesConfig implements WidgetConfig {
    public abstract String field();
    public abstract Boolean showDataTable();
    public abstract Boolean showPieChart();
    public abstract Integer limit();
    public abstract Integer dataTableLimit();
    public abstract String sortOrder();
    public abstract String stackedFields();

    @JsonCreator
    static QuickValuesConfig create(
            @JsonProperty("query") String query,
            @JsonProperty("timerange") TimeRange timerange,
            @JsonProperty("field") String field,
            @JsonProperty("show_data_table") Boolean showDataTable,
            @JsonProperty("show_pie_chart") Boolean showPieChart,
            @JsonProperty("limit") Integer limit,
            @JsonProperty("data_table_limit") Integer dataTableLimit,
            @JsonProperty("sort_order") String sortOrder,
            @JsonProperty("stacked_fields") String stackedFields
    ) {
        return new AutoValue_QuickValuesConfig(
                query,
                timerange,
                field,
                showDataTable,
                showPieChart,
                limit,
                dataTableLimit,
                sortOrder,
                stackedFields
        );
    }
}
