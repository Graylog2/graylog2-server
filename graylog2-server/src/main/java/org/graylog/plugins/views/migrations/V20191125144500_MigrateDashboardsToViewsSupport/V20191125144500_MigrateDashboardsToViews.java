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
package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport;

import org.graylog2.migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;

public class V20191125144500_MigrateDashboardsToViews extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20191125144500_MigrateDashboardsToViews.class);
    private final DashboardsService dashboardsService;

    @Inject
    public V20191125144500_MigrateDashboardsToViews(DashboardsService dashboardsService) {
        this.dashboardsService = dashboardsService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2019-11-25T14:45:00Z");
    }

    @Override
    public void upgrade() {
        this.dashboardsService.streamAll()
                .forEach(dashboard -> {
                    System.out.println("Hello dashboard " + dashboard.id());
                });
    }
}
