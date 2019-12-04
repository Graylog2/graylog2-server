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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Map;
import java.util.Set;

@AutoValue
@JsonAutoDetect
public abstract class MigrationCompleted {
    @JsonProperty("dashboard_to_view_migration_ids")
    public abstract Map<String, String> dashboardToViewMigrationIds();

    @JsonProperty("widget_migration_ids")
    public abstract Map<String, Set<String>> widgetMigrationIds();

    @JsonCreator
    static MigrationCompleted create(
            @JsonProperty("dashboard_to_view_migration_ids") Map<String, String> dashboardToViewMigrationIds,
            @JsonProperty("widget_migration_ids") Map<String, Set<String>> widgetMigrationIds
    ) {
        return new AutoValue_MigrationCompleted(dashboardToViewMigrationIds, widgetMigrationIds);
    }
}
