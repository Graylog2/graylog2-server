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

import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.graylog2.dashboards.widgets.DashboardWidgetCreator;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DashboardServiceImplTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public MongoConnectionRule mongoRule = MongoConnectionRule.build("test");
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private DashboardService dashboardService;

    @Mock
    private DashboardWidgetCreator dashboardWidgetCreator;

    @Before
    public void setUpService() throws Exception {
        this.dashboardService = new DashboardServiceImpl(mongoRule.getMongoConnection(), dashboardWidgetCreator);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void testCreate() throws Exception {
        final String title = "Dashboard Title";
        final String description = "This is the dashboard description";
        final String creatorUserId = "foobar";
        final DateTime createdAt = Tools.nowUTC();

        final Dashboard dashboard = dashboardService.create(title, description, creatorUserId, createdAt);

        assertNotNull(dashboard);
        assertEquals(title, dashboard.getTitle());
        assertEquals(description, dashboard.getDescription());
        assertNotNull(dashboard.getId());
        assertEquals(0, dashboardService.count());
    }

    @Test(expected = NotFoundException.class)
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void testLoadNonExistentDashboard() throws Exception {
        this.dashboardService.load("54e3deadbeefdeadbeefaffe");
    }

    @Test
    @UsingDataSet(locations = "singleDashboard.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testLoad() throws Exception {
        final String exampleDashboardId = "54e3deadbeefdeadbeefaffe";

        final Dashboard dashboard = dashboardService.load(exampleDashboardId);

        assertNotNull("Dashboard should have been found", dashboard);
        assertEquals("Dashboard id should be the one that was retrieved", exampleDashboardId, dashboard.getId());
    }

    @Test
    @UsingDataSet(locations = "singleDashboard.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testAll() throws Exception {
        final List<Dashboard> dashboards = dashboardService.all();
        final Dashboard dashboard = dashboards.get(0);

        assertEquals("Should have returned exactly 1 document", 1, dashboards.size());
        assertEquals("Example dashboard", dashboard.getTitle());
    }

    @Test
    public void testUpdateWidgetPositions() throws Exception {

    }

    @Test
    public void testAddWidget() throws Exception {

    }

    @Test
    public void testRemoveWidget() throws Exception {

    }

    @Test
    public void testUpdateWidgetDescription() throws Exception {

    }

    @Test
    public void testUpdateWidgetCacheTime() throws Exception {

    }

    @Test
    @UsingDataSet(locations = "singleDashboard.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testCountSingleDashboard() throws Exception {
        assertEquals(1, this.dashboardService.count());
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void testCountEmptyCollection() throws Exception {
        assertEquals(0, this.dashboardService.count());
    }
}
