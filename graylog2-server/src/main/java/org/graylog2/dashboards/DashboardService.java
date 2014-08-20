/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.dashboards;

import org.graylog2.dashboards.widgets.DashboardWidget;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PersistedService;
import org.graylog2.database.ValidationException;
import org.graylog2.rest.resources.dashboards.requests.WidgetPositionRequest;

import java.util.List;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public interface DashboardService extends PersistedService {
    Dashboard load(String id) throws NotFoundException;

    List<Dashboard> all();

    void updateWidgetPositions(Dashboard dashboard, List<WidgetPositionRequest> positions) throws ValidationException;

    void addWidget(Dashboard dashboard, DashboardWidget widget) throws ValidationException;

    void removeWidget(Dashboard dashboard, DashboardWidget widget);

    void updateWidgetDescription(Dashboard dashboard, DashboardWidget widget, String newDescription) throws ValidationException;

    void updateWidgetCacheTime(Dashboard dashboard, DashboardWidget widget, int cacheTime) throws ValidationException;
}
