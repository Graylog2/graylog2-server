/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.RandomUUIDProvider;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ViewWidget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ViewWidgetPosition;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.Widget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.WidgetPosition;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public interface WidgetConfig {
    default Set<ViewWidget> toViewWidgets(Widget widget, RandomUUIDProvider randomUUIDProvider) {
        throw new RuntimeException("Missing strategy to transform dashboard widget to view widget in class " + this.getClass().getSimpleName());
    }

    default Map<String, ViewWidgetPosition> toViewWidgetPositions(Set<ViewWidget> viewWidgets, WidgetPosition widgetPosition) {
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
