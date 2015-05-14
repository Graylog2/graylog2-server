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
package org.graylog2.rest.resources.system.collector;

import org.graylog2.collectors.CollectorService;
import org.graylog2.rest.models.collector.CollectorNodeDetailsSummary;
import org.graylog2.rest.models.collector.requests.CollectorRegistrationRequest;
import org.graylog2.rest.resources.RestResourceBaseTest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.graylog2.rest.assertj.ResponseAssert.assertThat;
import static org.mockito.Mockito.mock;

public class CollectorRegistrationResourceTest extends RestResourceBaseTest {
    private CollectorRegistrationResource resource;

    private CollectorService collectorService;

    @Before
    public void setUp() throws Exception {
        this.collectorService = mock(CollectorService.class);
        this.resource = new CollectorRegistrationResource(collectorService);
    }

    @Test
    public void testRegister() throws Exception {
        final CollectorRegistrationRequest input = CollectorRegistrationRequest.create("nodeId", CollectorNodeDetailsSummary.create("DummyOS 1.0"));

        final Response response = this.resource.register("collectorId", input, "0.0.1");

        assertThat(response).isSuccess();
    }

    @Test
    @Ignore
    public void testRegisterInvalidCollectorId() throws Exception {
        final CollectorRegistrationRequest invalid = CollectorRegistrationRequest.create("nodeId", CollectorNodeDetailsSummary.create("DummyOS 1.0"));

        final Response response = this.resource.register("", invalid, "0.0.1");

        assertThat(response).isError();
        assertThat(response).isStatus(Response.Status.BAD_REQUEST);
    }

    @Test
    @Ignore
    public void testRegisterInvalidNodeId() throws Exception {
        final CollectorRegistrationRequest invalid = CollectorRegistrationRequest.create("", CollectorNodeDetailsSummary.create("DummyOS 1.0"));

        final Response response = this.resource.register("collectorId", invalid, "0.0.1");

        assertThat(response).isError();
        assertThat(response).isStatus(Response.Status.BAD_REQUEST);
    }

    @Test
    @Ignore
    public void testRegisterMissingNodeDetails() throws Exception {
        final CollectorRegistrationRequest invalid = CollectorRegistrationRequest.create("nodeId", null);

        final Response response = this.resource.register("collectorId", invalid, "0.0.1");

        assertThat(response).isError();
        assertThat(response).isStatus(Response.Status.BAD_REQUEST);
    }

    @Test
    @Ignore
    public void testRegisterMissingOperatingSystem() throws Exception {
        final CollectorRegistrationRequest invalid = CollectorRegistrationRequest.create("nodeId", CollectorNodeDetailsSummary.create(""));

        final Response response = this.resource.register("collectorId", invalid, "0.0.1");

        assertThat(response).isError();
        assertThat(response).isStatus(Response.Status.BAD_REQUEST);
    }
}