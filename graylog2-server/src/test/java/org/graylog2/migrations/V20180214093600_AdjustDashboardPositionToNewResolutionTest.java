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

import com.google.common.collect.ImmutableList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import org.graylog2.dashboards.Dashboard;
import org.graylog2.dashboards.DashboardService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

public class V20180214093600_AdjustDashboardPositionToNewResolutionTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private V20180214093600_AdjustDashboardPositionToNewResolution adjustDashboardResolutionMigration;

    @Mock
    private ClusterConfigService clusterConfigService;

    @Mock
    private DashboardService dashboardService;


    @Mock
    private DBCollection dbCollection;

    @Before
    public void setUp() throws Exception {
        this.clusterConfigService = mock(ClusterConfigService.class);
        when(clusterConfigService.get(any())).thenReturn(null);
        this.adjustDashboardResolutionMigration = new V20180214093600_AdjustDashboardPositionToNewResolution(
                dashboardService,
                clusterConfigService);
    }

    @Test
    public void doNotMigrateAnythingWithoutDashboards() throws Exception {
      when(this.dashboardService.all()).thenReturn(Collections.emptyList());

      this.adjustDashboardResolutionMigration.upgrade();

      verify(this.dashboardService, never()).save(any());
    }

    @Test
    public void doNotMigrateAnythingWithDashboardsWithoutPositions() throws Exception {
        final BasicDBObject fields = mock(BasicDBObject.class);
        final Dashboard dashboard = mock(Dashboard.class);
        when(dashboard.getId()).thenReturn("uuu-iii-ddd");
        when(dashboard.getFields()).thenReturn(fields);
        when(this.dashboardService.all()).thenReturn(ImmutableList.of(dashboard));

        this.adjustDashboardResolutionMigration.upgrade();

        verify(this.dashboardService, never()).save(any());
    }

    @Test
    public void doMigrateOneDashboardsPositions() throws Exception {

        final BasicDBObject position = mock(BasicDBObject.class);
        when(position.get("width")).thenReturn(5);
        when(position.get("height")).thenReturn(4);
        when(position.get("col")).thenReturn(1);
        when(position.get("row")).thenReturn(1);

        final BasicDBObject positions = mock(BasicDBObject.class);
        when(positions.get(any())).thenReturn(position);
        Set keySet = new java.util.HashSet();
        keySet.add("my-position-id");
        when(positions.keySet()).thenReturn(keySet);

        final BasicDBObject fields = mock(BasicDBObject.class);
        when(fields.get(any())).thenReturn(positions);

        final Dashboard dashboard = mock(Dashboard.class, Mockito.RETURNS_DEEP_STUBS);
        when(dashboard.getFields()).thenReturn(fields);
        when(dashboard.getId()).thenReturn("uuu-iii-ddd");
        when(this.dashboardService.all()).thenReturn(ImmutableList.of(dashboard));

        this.adjustDashboardResolutionMigration.upgrade();

        verify(this.dashboardService, times(1)).save(dashboard);
    }
}
