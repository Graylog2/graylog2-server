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

import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.dashboards.widgets.DashboardWidgetCreator;
import org.graylog2.shared.SuppressForbidden;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;

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
        dashboardService = new DashboardServiceImpl(mongodb.mongoConnection());
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
    @MongoDBFixtures("singleDashboard.json")
    public void testCountSingleDashboard() throws Exception {
        assertEquals(1, this.dashboardService.count());
    }

    @Test
    public void testCountEmptyCollection() throws Exception {
        assertEquals(0, this.dashboardService.count());
    }
}
