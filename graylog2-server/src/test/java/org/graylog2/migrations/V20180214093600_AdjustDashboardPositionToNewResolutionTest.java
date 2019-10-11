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
import org.graylog2.dashboards.widgets.WidgetPosition;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

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
        when(dashboard.getPositions()).thenReturn(Collections.emptyList());
        when(this.dashboardService.all()).thenReturn(Collections.singletonList(dashboard));

        this.adjustDashboardResolutionMigration.upgrade();

        verify(this.dashboardService, never()).save(any());
    }

    @Test
    public void doMigrateOneDashboardsPositions() throws Exception {
        /* Dashboard 1 */
        final List<WidgetPosition> oldPositions1 = Collections.singletonList(
                WidgetPosition.builder().id("my-position-id").width(5).height(4).col(2).row(2).build());

        final List<WidgetPosition> newPositions1 = Collections.singletonList(
                WidgetPosition.builder().id("my-position-id").width(10).height(8).col(3).row(3).build());

        final Dashboard dashboard1 = mock(Dashboard.class);
        when(dashboard1.getPositions()).thenReturn(oldPositions1);
        when(dashboard1.getId()).thenReturn("uuu-iii-ddd");

        /* Dashboard 2 */
        final List<WidgetPosition> oldPositions2 = Collections.singletonList(
                WidgetPosition.builder().id("your-position-id").width(1).height(1).col(1).row(1).build());

        final List<WidgetPosition> newPositions2 = Collections.singletonList(
                WidgetPosition.builder().id("your-position-id").width(2).height(2).col(1).row(1).build());

        final Dashboard dashboard2 = mock(Dashboard.class);
        when(dashboard2.getPositions()).thenReturn(oldPositions2);
        when(dashboard2.getId()).thenReturn("uuu-iii-eee");

        /* Dashboard 3 */
        final List<WidgetPosition> oldPositions3 = ImmutableList.of(
                WidgetPosition.builder().id("his-position-id").width(1).height(1).col(1).row(1).build(),
                WidgetPosition.builder().id("her-position-id").width(2).height(2).col(2).row(2).build());

        final List<WidgetPosition> newPositions3 = ImmutableList.of(
                WidgetPosition.builder().id("his-position-id").width(2).height(2).col(1).row(1).build(),
                WidgetPosition.builder().id("her-position-id").width(4).height(4).col(3).row(3).build());

        final Dashboard dashboard3 = mock(Dashboard.class);
        when(dashboard3.getPositions()).thenReturn(oldPositions3);
        when(dashboard3.getId()).thenReturn("uuu-iii-eee");

        List<Dashboard> dashboards = ImmutableList.of(dashboard1, dashboard2, dashboard3);
        when(this.dashboardService.all()).thenReturn(dashboards);

        this.adjustDashboardResolutionMigration.upgrade();

        verify(dashboard1).setPositions(newPositions1);
        verify(this.dashboardService, times(1)).save(dashboard1);
        verify(dashboard2).setPositions(newPositions2);
        verify(this.dashboardService, times(1)).save(dashboard2);
        verify(dashboard3).setPositions(newPositions3);
        verify(this.dashboardService, times(1)).save(dashboard3);
    }
}
