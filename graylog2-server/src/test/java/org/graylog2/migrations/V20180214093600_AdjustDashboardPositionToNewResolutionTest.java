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
import java.util.HashMap;
import java.util.Map;

public class V20180214093600_AdjustDashboardPositionToNewResolutionTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private V20180214093600_AdjustDashboardPositionToNewResolution adjustDashboardResolutionMigration;

    @Mock
    private ClusterConfigService clusterConfigService;

    @Mock
    private DashboardService dashboardService;

    @Before
    public void setUp() throws Exception {
        this.clusterConfigService = mock(ClusterConfigService.class);
        when(clusterConfigService.get(any())).thenReturn(null);
        this.adjustDashboardResolutionMigration = new V20180214093600_AdjustDashboardPositionToNewResolution(
                dashboardService,
                clusterConfigService
        );
    }

    @Test
    public void doNotMigrateAnythingWithoutDashboards() throws Exception {
        when(this.dashboardService.all()).thenReturn(Collections.emptyList());

        this.adjustDashboardResolutionMigration.upgrade();

        verify(this.dashboardService, never()).save(any());
    }

    @Test
    public void doNotMigrateAnythingWithDashboardsWithoutPositions() throws Exception {
        final Dashboard dashboard = mock(Dashboard.class);
        when(dashboard.getId()).thenReturn("uuu-iii-ddd");
        when(dashboard.getPositions()).thenReturn(null);
        when(this.dashboardService.all()).thenReturn(ImmutableList.of(dashboard));

        this.adjustDashboardResolutionMigration.upgrade();

        verify(this.dashboardService, never()).save(any());
    }

    @Test
    public void doMigrateOneDashboardsPositions() throws Exception {

        final Map<String, Map<String, Integer>> oldPositions = new HashMap<>(1);
        final Map<String, Integer> position = new HashMap<>(4);
        position.put("width", 5);
        position.put("height", 4);
        position.put("col", 2);
        position.put("row", 2);
        oldPositions.put("my-position-id", position);

        final Map<String, Map<String, Object>> newPositions = new HashMap<>(1);
        final Map<String, Object> newPosition = new HashMap<>(4);
        newPosition.put("width", 10);
        newPosition.put("height", 8);
        newPosition.put("col", 3);
        newPosition.put("row", 3);
        newPositions.put("my-position-id", newPosition);

        final Dashboard dashboard = mock(Dashboard.class, Mockito.RETURNS_DEEP_STUBS);
        when(dashboard.getPositions()).thenReturn(oldPositions);
        when(dashboard.getId()).thenReturn("uuu-iii-ddd");
        when(this.dashboardService.all()).thenReturn(ImmutableList.of(dashboard));

        this.adjustDashboardResolutionMigration.upgrade();

        verify(dashboard).setPostions(newPositions);
        verify(this.dashboardService, times(1)).save(dashboard);
    }
}
