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
package org.graylog2.migrations;

import com.google.common.collect.Maps;

import com.mongodb.BasicDBObject;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.dashboards.Dashboard;
import org.graylog2.dashboards.DashboardImpl;
import org.graylog2.dashboards.DashboardService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.auto.value.AutoValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.Map;

import javax.inject.Inject;

import static java.lang.Integer.parseInt;

/**
 * Migration adjusting the position of dashboard widgets to the higher resolution of the
 * grid layout.
 */
public class V20180214093600_AdjustDashboardPositionToNewResolution extends Migration {

    private static final Logger LOG = LoggerFactory.getLogger(V20180214093600_AdjustDashboardPositionToNewResolution.class);
    private final DashboardService dashboardService;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20180214093600_AdjustDashboardPositionToNewResolution(DashboardService dashboardService,
                                                                ClusterConfigService clusterConfigService) {
        this.dashboardService = dashboardService;
        this.clusterConfigService = clusterConfigService;
    }


    public ZonedDateTime createdAt() { return ZonedDateTime.parse("2018-02-14T09:36:00Z"); }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed!");
            return;
        }

        Map<String, String> dashboardIds = Maps.newHashMap();
        for (Dashboard dashboard : dashboardService.all()) {
            Map<String, Object> fields = dashboard.getFields();
            BasicDBObject positions = (BasicDBObject) fields.get(DashboardImpl.EMBEDDED_POSITIONS);
            if (positions == null) {
                dashboardIds.put(dashboard.getId(), "skipped");
                continue;
            }
            Map<String, Map<String, Object>> newPosition = Maps.newHashMap();

            for ( String positionId : positions.keySet() ) {
                BasicDBObject position = (BasicDBObject) positions.get(positionId);
                Integer newWidth = parseInt(position.get("width").toString()) * 2;
                Integer newHeight = parseInt(position.get("height").toString()) * 2;
                Integer newCol = adjustPosition(parseInt(position.get("col").toString()));
                Integer newRow = adjustPosition(parseInt(position.get("row").toString()));

                Map<String, Object> x = Maps.newHashMap();
                x.put("col", newCol);
                x.put("row", newRow);
                x.put("height", newHeight);
                x.put("width", newWidth);

                newPosition.put(positionId, x);
            }
            try {
                dashboard.getFields().put(DashboardImpl.EMBEDDED_POSITIONS, newPosition);
                dashboardService.save(dashboard);
                dashboardIds.put(dashboard.getId(), "updated");
            } catch (ValidationException e) {
                LOG.error("Could not update dashboard position: {}", e);
            }
        }
        clusterConfigService.write(MigrationCompleted.create(dashboardIds));
    }

    /* We double the resolution in space starting with 1.
       To keep widgets on the same position we need to subtract 1 from the result. */
    private Integer adjustPosition(Integer value) {
        return value * 2 - 1;
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public static abstract class MigrationCompleted {
        @JsonProperty("dashboard_ids")
        public abstract Map<String, String> dashboard_ids();

        @JsonCreator
        public static MigrationCompleted create(@JsonProperty("dashboard_ids")Map<String, String> dashboardIds) {
            return new AutoValue_V20180214093600_AdjustDashboardPositionToNewResolution_MigrationCompleted(dashboardIds);
        }
    }
}
