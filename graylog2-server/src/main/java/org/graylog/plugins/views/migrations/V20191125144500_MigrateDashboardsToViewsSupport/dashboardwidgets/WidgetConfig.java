package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.TimeRange;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = FieldChartConfig.class, name = "FIELD_CHART"),
        @JsonSubTypes.Type(value = SearchResultChartConfig.class, name = "SEARCH_RESULT_CHART"),
        @JsonSubTypes.Type(value = SearchResultCountConfig.class, name = "SEARCH_RESULT_COUNT"),
        @JsonSubTypes.Type(value = QuickValuesConfig.class, name = "QUICKVALUES"),
        @JsonSubTypes.Type(value = QuickValuesHistogramConfig.class, name = "QUICKVALUES_HISTOGRAM")
})
public interface WidgetConfig {
    @JsonProperty
    String query();

    @JsonProperty
    TimeRange timerange();
}
