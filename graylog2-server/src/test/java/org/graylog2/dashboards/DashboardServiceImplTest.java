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

import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.dashboards.widgets.DashboardWidgetCreator;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.shared.SuppressForbidden;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class DashboardServiceImplTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private DashboardService dashboardService;

    @Mock
    private DashboardWidgetCreator dashboardWidgetCreator;

    @Before
    @SuppressForbidden("Using Executors.newSingleThreadExecutor() is okay in tests")
    public void setUpService() {
        final ClusterEventBus clusterEventBus = new ClusterEventBus("cluster-event-bus", Executors.newSingleThreadExecutor());
        final EventBus serverEventBus = new EventBus("server-event-bus");
        dashboardService = new DashboardServiceImpl(
                mongodb.mongoConnection(),
                dashboardWidgetCreator,
                clusterEventBus,
                serverEventBus);
    }

    @Test
    @MongoDBFixtures("singleDashboard.json")
    public void testAll() {
        final List<Dashboard> dashboards = dashboardService.all();
        final Dashboard dashboard = dashboards.get(0);

        assertEquals("Should have returned exactly 1 document", 1, dashboards.size());
        assertEquals("Example dashboard", dashboard.getTitle());
    }

    @Test
    @MongoDBFixtures("DashboardServiceImplTest.json")
    public void testLoadByIds() {
        assertThat(dashboardService.loadByIds(ImmutableSet.of())).isEmpty();
        assertThat(dashboardService.loadByIds(ImmutableSet.of("54e300000000000000000000"))).isEmpty();
        assertThat(dashboardService.loadByIds(ImmutableSet.of("54e3deadbeefdeadbeef0001"))).hasSize(1);
        assertThat(dashboardService.loadByIds(ImmutableSet.of("54e3deadbeefdeadbeef0001", "54e3deadbeefdeadbeef0001"))).hasSize(1);
        assertThat(dashboardService.loadByIds(ImmutableSet.of("54e3deadbeefdeadbeef0001", "54e3deadbeefdeadbeef0002", "54e300000000000000000000"))).hasSize(2);
    }

    @Test
    @MongoDBFixtures("singleDashboard.json")
    public void testCountSingleDashboard() throws Exception {
        assertEquals(1, this.dashboardService.count());
    }

    @Test
    public void testCountEmptyCollection() throws Exception {
        assertEquals(0, this.dashboardService.count());
    }
}
