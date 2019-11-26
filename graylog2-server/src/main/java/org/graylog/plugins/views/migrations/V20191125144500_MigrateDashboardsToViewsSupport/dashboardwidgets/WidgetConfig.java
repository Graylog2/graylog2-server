package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.TimeRange;

public interface WidgetConfig {
    @JsonProperty
    String query();

    @JsonProperty
    TimeRange timerange();
}
