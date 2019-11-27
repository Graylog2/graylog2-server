package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.TimeRange;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ViewWidget;

import java.util.Set;

public interface WidgetConfig {
    @JsonProperty
    String query();

    @JsonProperty
    TimeRange timerange();

    default Set<ViewWidget> toViewWidgets() {
        throw new RuntimeException("Missing strategy to transform dashboard widget to view widget in class " + this.getClass().getSimpleName());
    }
}
