package org.graylog2.migrations;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;

public class V20230113095300_MigrateGlobalPivotLimitsToGroupingsInSearches extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20230113095300_MigrateGlobalPivotLimitsToGroupingsInViews.class);
    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20230113095300_MigrateGlobalPivotLimitsToGroupingsInSearches(ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2023-01-13T09:53:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(V20220930095323_MigratePivotLimitsInSearches.MigrationCompleted.class) == null) {
            LOG.debug("Previous migration did not run - no need to migrate!");
            return;
        }
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed!");
        }

    }

    public record MigrationCompleted(@JsonProperty("migrated_search_types") Integer migratedSearchTypes) {}
}
