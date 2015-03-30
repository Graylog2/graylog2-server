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
        final AgentRegistrationRequest input = AgentRegistrationRequest.create("foobar", "nodeId", AgentNodeDetails.create("DummyOS 1.0"));

        final Response response = this.resource.register(input);

        assertThat(response).isSuccess();
    }
}