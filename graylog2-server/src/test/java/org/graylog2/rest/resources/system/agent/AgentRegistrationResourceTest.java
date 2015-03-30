package org.graylog2.rest.resources.system.agent;

import org.graylog2.rest.models.agent.AgentNodeDetails;
import org.graylog2.rest.models.agent.requests.AgentRegistrationRequest;
import org.graylog2.rest.resources.RestResourceBaseTest;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.graylog2.rest.assertj.ResponseAssert.assertThat;

public class AgentRegistrationResourceTest extends RestResourceBaseTest {
    private AgentRegistrationResource resource;

    @Before
    public void setUp() throws Exception {
        this.resource = new AgentRegistrationResource();
    }

    @Test
    public void testRegister() throws Exception {
        final AgentRegistrationRequest input = AgentRegistrationRequest.create("agentId", "nodeId", AgentNodeDetails.create("DummyOS 1.0"));

        final Response response = this.resource.register(input);

        assertThat(response).isSuccess();
    }

    @Test
    public void testRegisterInvalidAgentId() throws Exception {
        final AgentRegistrationRequest invalid = AgentRegistrationRequest.create("", "nodeId", AgentNodeDetails.create("DummyOS 1.0"));

        final Response response = this.resource.register(invalid);

        assertThat(response).isError();
        assertThat(response).isStatus(Response.Status.BAD_REQUEST);
    }

    @Test
    public void testRegisterInvalidNodeId() throws Exception {
        final AgentRegistrationRequest invalid = AgentRegistrationRequest.create("agentId", "", AgentNodeDetails.create("DummyOS 1.0"));

        final Response response = this.resource.register(invalid);

        assertThat(response).isError();
        assertThat(response).isStatus(Response.Status.BAD_REQUEST);
    }

    @Test
    public void testRegisterMissingNodeDetails() throws Exception {
        final AgentRegistrationRequest invalid = AgentRegistrationRequest.create("agentId", "nodeId", null);

        final Response response = this.resource.register(invalid);

        assertThat(response).isError();
        assertThat(response).isStatus(Response.Status.BAD_REQUEST);
    }

    @Test
    public void testRegisterMissingOperatingSystem() throws Exception {
        final AgentRegistrationRequest invalid = AgentRegistrationRequest.create("agentId", "nodeId", AgentNodeDetails.create(""));

        final Response response = this.resource.register(invalid);

        assertThat(response).isError();
        assertThat(response).isStatus(Response.Status.BAD_REQUEST);
    }
}