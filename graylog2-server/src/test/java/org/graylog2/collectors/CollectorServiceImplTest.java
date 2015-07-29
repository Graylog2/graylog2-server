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
package org.graylog2.collectors;

import com.lordofthejars.nosqlunit.annotation.CustomComparisonStrategy;
import com.lordofthejars.nosqlunit.annotation.IgnorePropertyValue;
import com.lordofthejars.nosqlunit.annotation.ShouldMatchDataSet;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import com.lordofthejars.nosqlunit.mongodb.MongoFlexibleComparisonStrategy;
import org.graylog2.bindings.ServerObjectMapperModule;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.shared.bindings.ValidatorModule;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.validation.Validator;
import java.util.List;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JukitoRunner.class)
@UseModules({ServerObjectMapperModule.class, ValidatorModule.class})
@CustomComparisonStrategy(comparisonStrategy = MongoFlexibleComparisonStrategy.class)
public class CollectorServiceImplTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    private CollectorService collectorService;

    @Before
    public void setUp(MongoJackObjectMapperProvider mapperProvider,
                      Validator validator) throws Exception {
        this.collectorService = new CollectorServiceImpl(mongoRule.getMongoConnection(), mapperProvider, validator);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void testCountEmptyCollection() throws Exception {
        final long result = this.collectorService.count();

        assertEquals(0, result);
    }

    @Test
    @UsingDataSet(locations = "collectorsMultipleDocuments.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testCountNonEmptyCollection() throws Exception {
        final long result = this.collectorService.count();

        assertEquals(3, result);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    @ShouldMatchDataSet(location = "collectorsSingleDataset.json")
    @IgnorePropertyValue(properties = {"_id", "last_seen"})
    public void testSaveFirstRecord() throws Exception {
        final Collector collector = CollectorImpl.create("collectorId", "nodeId", "0.0.1", CollectorNodeDetails.create("DummyOS 1.0"), DateTime.now(DateTimeZone.UTC));

        final Collector result = this.collectorService.save(collector);

        assertNotNull(result);
    }

    @Test
    @UsingDataSet(locations = "collectorsMultipleDocuments.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testAll() throws Exception {
        final List<Collector> collectors = this.collectorService.all();

        assertNotNull(collectors);
        assertEquals(3, collectors.size());
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void testAllEmptyCollection() throws Exception {
        final List<Collector> collectors = this.collectorService.all();

        assertNotNull(collectors);
        assertEquals(0, collectors.size());
    }

    @Test
    @UsingDataSet(locations = "collectorsMultipleDocuments.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testFindById() throws Exception {
        final String collector1id = "collector1id";

        final Collector collector = this.collectorService.findById(collector1id);

        assertNotNull(collector);
        assertEquals(collector1id, collector.getId());
    }

    @Test
    @UsingDataSet(locations = "collectorsMultipleDocuments.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testFindByIdNonexisting() throws Exception {
        final String collector1id = "nonexisting";

        final Collector collector = this.collectorService.findById(collector1id);

        assertNull(collector);
    }

    @Test
    @UsingDataSet(locations = "collectorsMultipleDocuments.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testFindByNodeId() throws Exception {
        final String nodeId = "uniqueid1";

        final List<Collector> collectors = this.collectorService.findByNodeId(nodeId);

        assertNotNull(collectors);
        assertEquals(1, collectors.size());

        for (Collector collector : collectors) {
            assertNotNull(collector);
            assertEquals(nodeId, collector.getNodeId());
        }
    }

    @Test
    @UsingDataSet(locations = "collectorsMultipleDocuments.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testFindByNodeIdNonexisting() throws Exception {
        final String nodeId = "nonexisting";

        final List<Collector> collectors = this.collectorService.findByNodeId(nodeId);

        assertNotNull(collectors);
        assertEquals(0, collectors.size());
    }

    @Test
    @UsingDataSet(locations = "collectorsMultipleDocuments.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    @ShouldMatchDataSet
    public void testDestroy() throws Exception {
        final Collector collector = mock(Collector.class);
        when(collector.getId()).thenReturn("collector2id");

        final int result = this.collectorService.destroy(collector);

        assertEquals(1, result);
    }
}