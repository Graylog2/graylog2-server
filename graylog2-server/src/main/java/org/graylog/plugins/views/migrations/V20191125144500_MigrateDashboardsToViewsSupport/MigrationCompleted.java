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
