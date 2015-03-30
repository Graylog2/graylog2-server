package org.graylog2.rest.resources.system.agent;

import com.google.common.collect.Lists;
import org.graylog2.agents.Agent;
import org.graylog2.rest.models.agent.responses.AgentList;
import org.graylog2.rest.models.agent.responses.AgentSummary;
import org.graylog2.rest.resources.RestResourceBaseTest;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AgentResourceTest extends RestResourceBaseTest {
    private AgentResource resource;
    private List<Agent> agents;

    @Before
    public void setUp() throws Exception {
        this.agents = getDummyAgentList();
        this.resource = new AgentResource();
    }

    @Test
    public void testList() throws Exception {
        final AgentList response = this.resource.list();

        assertNotNull(response);
        assertNotNull(response.agents());
        assertEquals("Agent list should be of same size as dummy list", agents.size(), response.agents().size());
    }

    @Test
    public void testGetNotExisting() throws Exception {
        final AgentSummary response = this.resource.get("Nonexisting");

        assertNull(response);
    }

    @Test
    public void testGet() throws Exception {
        final Agent agent = agents.get(agents.size() - 1);
        final AgentSummary response = this.resource.get(agent.getId());

        assertNotNull(response);
        assertEquals(agent.getId(), response.id());
        assertEquals(agent.getNodeId(), response.nodeId());
        assertNotNull(response.nodeDetails());
        assertEquals(agent.getOperatingSystem(), response.nodeDetails().operatingSystem());
        assertEquals(agent.getLastSeen(), response.lastSeen());
    }

    private Agent getDummyAgent(String id, String nodeId, DateTime lastSeen, String operatingSystem) {
        final Agent agent = mock(Agent.class);
        when(agent.getId()).thenReturn(id);
        when(agent.getNodeId()).thenReturn(nodeId);
        when(agent.getLastSeen()).thenReturn(lastSeen);
        when(agent.getOperatingSystem()).thenReturn(operatingSystem);

        return agent;
    }
    private List<Agent> getDummyAgentList() {
        final Agent agent1 = getDummyAgent("agent1id", "agent1nodeid", DateTime.now(), "DummyOS 1.0");
        final Agent agent2 = getDummyAgent("agent2id", "agent2nodeid", DateTime.now(), "DummyOS 1.0");
        final Agent agent3 = getDummyAgent("agent3id", "agent3nodeid", DateTime.now(), "DummyOS 1.0");

        return Lists.newArrayList(agent1, agent2, agent3);
    }
}