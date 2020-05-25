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
import org.graylog2.plugin.database.PersistedService;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface DashboardService extends PersistedService {
    List<Dashboard> all();

    Set<Dashboard> loadByIds(Collection<String> ids);

    void removeWidget(Dashboard dashboard, DashboardWidget widget);
    /**
     * @return the total number of dashboards
     */
    long count();

    int destroy(Dashboard model);
}
