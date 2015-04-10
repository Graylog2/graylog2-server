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
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.database.PersistedService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.rest.models.dashboards.requests.WidgetPositionsRequest;
import org.joda.time.DateTime;

import java.util.List;

public interface DashboardService extends PersistedService {
    Dashboard create(String title, String description, String creatorUserId, DateTime createdAt);
    Dashboard load(String id) throws NotFoundException;

    List<Dashboard> all();

    void updateWidgetPositions(Dashboard dashboard, WidgetPositionsRequest positions) throws ValidationException;

    void addWidget(Dashboard dashboard, DashboardWidget widget) throws ValidationException;

    void removeWidget(Dashboard dashboard, DashboardWidget widget);

    @Deprecated
    void updateWidgetDescription(Dashboard dashboard, DashboardWidget widget, String newDescription) throws ValidationException;

    @Deprecated
    void updateWidgetCacheTime(Dashboard dashboard, DashboardWidget widget, int cacheTime) throws ValidationException;

    /**
     * @return the total number of dashboards
     */
    long count();
}
