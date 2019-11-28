package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.TimeRange;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ViewWidget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ViewWidgetPosition;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.Widget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.WidgetPosition;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public interface WidgetConfig {
    TimeRange timerange();

    default Set<ViewWidget> toViewWidgets() {
        throw new RuntimeException("Missing strategy to transform dashboard widget to view widget in class " + this.getClass().getSimpleName());
    }

    default Map<String, ViewWidgetPosition> toViewWidgetPositions(Set<ViewWidget> viewWidgets, Widget oldWidget, WidgetPosition widgetPosition) {
        final ViewWidgetPosition newPosition = ViewWidgetPosition.builder()
                .col(widgetPosition.col())
                .row(widgetPosition.row())
                .height(widgetPosition.height())
                .width(widgetPosition.width())
                .build();
        return viewWidgets.stream()
                .collect(Collectors.toMap(ViewWidget::id, viewWidget -> newPosition));
    }
}
