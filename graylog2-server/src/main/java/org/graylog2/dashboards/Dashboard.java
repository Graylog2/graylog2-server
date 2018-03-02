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
package org.graylog2.dashboards;

import org.graylog2.dashboards.widgets.DashboardWidget;
import org.graylog2.dashboards.widgets.WidgetPosition;
import org.graylog2.plugin.database.Persisted;

import java.util.List;
import java.util.Map;

public interface Dashboard extends Persisted {
    String getTitle();

    void setTitle(String title);

    List<WidgetPosition> getPositions();

    void setPositions(List<WidgetPosition> widgetPositions);

    String getDescription();

    void setDescription(String description);

    String getContentPack();

    void setContentPack(String contentPack);

    void addPersistedWidget(DashboardWidget widget);

    DashboardWidget getWidget(String widgetId);

    DashboardWidget addWidget(DashboardWidget widget);

    DashboardWidget removeWidget(DashboardWidget widget);

    Map<String, DashboardWidget> getWidgets();
}
