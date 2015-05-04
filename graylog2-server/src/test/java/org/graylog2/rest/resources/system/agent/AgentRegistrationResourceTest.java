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
package org.graylog2.rest.resources.system.agent;

import org.graylog2.agents.AgentService;
import org.graylog2.rest.models.agent.AgentNodeDetailsSummary;
import org.graylog2.rest.models.agent.requests.AgentRegistrationRequest;
import org.graylog2.rest.resources.RestResourceBaseTest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.graylog2.rest.assertj.ResponseAssert.assertThat;
import static org.mockito.Mockito.mock;

public class AgentRegistrationResourceTest extends RestResourceBaseTest {
    private AgentRegistrationResource resource;

    private AgentService agentService;

    @Before
    public void setUp() throws Exception {
        this.agentService = mock(AgentService.class);
        this.resource = new AgentRegistrationResource(agentService);
    }

    @Test
    public void testRegister() throws Exception {
        final AgentRegistrationRequest input = AgentRegistrationRequest.create("nodeId", AgentNodeDetailsSummary.create("DummyOS 1.0"));

        final Response response = this.resource.register("agentId", input, "0.0.1");

        assertThat(response).isSuccess();
    }

    @Test
    @Ignore
    public void testRegisterInvalidAgentId() throws Exception {
        final AgentRegistrationRequest invalid = AgentRegistrationRequest.create("nodeId", AgentNodeDetailsSummary.create("DummyOS 1.0"));

        final Response response = this.resource.register("", invalid, "0.0.1");

        assertThat(response).isError();
        assertThat(response).isStatus(Response.Status.BAD_REQUEST);
    }

    @Test
    @Ignore
    public void testRegisterInvalidNodeId() throws Exception {
        final AgentRegistrationRequest invalid = AgentRegistrationRequest.create("", AgentNodeDetailsSummary.create("DummyOS 1.0"));

        final Response response = this.resource.register("agentId", invalid, "0.0.1");

        assertThat(response).isError();
        assertThat(response).isStatus(Response.Status.BAD_REQUEST);
    }

    @Test
    @Ignore
    public void testRegisterMissingNodeDetails() throws Exception {
        final AgentRegistrationRequest invalid = AgentRegistrationRequest.create("nodeId", null);

        final Response response = this.resource.register("agentId", invalid, "0.0.1");

        assertThat(response).isError();
        assertThat(response).isStatus(Response.Status.BAD_REQUEST);
    }

    @Test
    @Ignore
    public void testRegisterMissingOperatingSystem() throws Exception {
        final AgentRegistrationRequest invalid = AgentRegistrationRequest.create("nodeId", AgentNodeDetailsSummary.create(""));

        final Response response = this.resource.register("agentId", invalid, "0.0.1");

        assertThat(response).isError();
        assertThat(response).isStatus(Response.Status.BAD_REQUEST);
    }
}