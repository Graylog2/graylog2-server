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
package org.graylog2.agents;

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
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JukitoRunner.class)
@UseModules({ServerObjectMapperModule.class, ValidatorModule.class})
@CustomComparisonStrategy(comparisonStrategy = MongoFlexibleComparisonStrategy.class)
public class AgentServiceImplTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    private AgentService agentService;

    @Before
    public void setUp(MongoJackObjectMapperProvider mapperProvider,
                      Validator validator) throws Exception {
        this.agentService = new AgentServiceImpl(mongoRule.getMongoConnection(), mapperProvider, validator);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void testCountEmptyCollection() throws Exception {
        final long result = this.agentService.count();

        assertEquals(0, result);
    }

    @Test
    @UsingDataSet(locations = "agentsMultipleDocuments.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testCountNonEmptyCollection() throws Exception {
        final long result = this.agentService.count();

        assertEquals(3, result);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    @ShouldMatchDataSet(location = "agentsSingleDataset.json")
    @IgnorePropertyValue(properties = {"_id", "last_seen"})
    public void testSaveFirstRecord() throws Exception {
        final Agent agent = AgentImpl.create("agentId", "nodeId", AgentNodeDetails.create("DummyOS 1.0"), DateTime.now());

        final Agent result = this.agentService.save(agent);

        assertNotNull(result);
    }

    @Test
    @UsingDataSet(locations = "agentsMultipleDocuments.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testAll() throws Exception {
        final List<Agent> agents = this.agentService.all();

        assertNotNull(agents);
        assertEquals(3, agents.size());
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void testAllEmptyCollection() throws Exception {
        final List<Agent> agents = this.agentService.all();

        assertNotNull(agents);
        assertEquals(0, agents.size());
    }

    @Test
    @UsingDataSet(locations = "agentsMultipleDocuments.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testFindById() throws Exception {
        final String agent1id = "agent1id";

        final Agent agent = this.agentService.findById(agent1id);

        assertNotNull(agent);
        assertEquals(agent1id, agent.getId());
    }

    @Test
    @UsingDataSet(locations = "agentsMultipleDocuments.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testFindByIdNonexisting() throws Exception {
        final String agent1id = "nonexisting";

        final Agent agent = this.agentService.findById(agent1id);

        assertNull(agent);
    }

    @Test
    @UsingDataSet(locations = "agentsMultipleDocuments.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testFindByNodeId() throws Exception {
        final String nodeId = "uniqueid1";

        final List<Agent> agents = this.agentService.findByNodeId(nodeId);

        assertNotNull(agents);
        assertEquals(1, agents.size());

        for (Agent agent : agents) {
            assertNotNull(agent);
            assertEquals(nodeId, agent.getNodeId());
        }
    }

    @Test
    @UsingDataSet(locations = "agentsMultipleDocuments.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testFindByNodeIdNonexisting() throws Exception {
        final String nodeId = "nonexisting";

        final List<Agent> agents = this.agentService.findByNodeId(nodeId);

        assertNotNull(agents);
        assertEquals(0, agents.size());
    }

    @Test
    @UsingDataSet(locations = "agentsMultipleDocuments.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    @ShouldMatchDataSet
    public void testDestroy() throws Exception {
        final Agent agent = mock(Agent.class);
        when(agent.getId()).thenReturn("agent2id");

        final int result = this.agentService.destroy(agent);

        assertEquals(1, result);
    }
}