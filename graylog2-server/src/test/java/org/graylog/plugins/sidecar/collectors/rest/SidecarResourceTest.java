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
package org.graylog.plugins.sidecar.collectors.rest;

import com.google.common.collect.Lists;
import org.graylog.plugins.sidecar.collectors.rest.resources.RestResourceBaseTest;
import org.graylog.plugins.sidecar.filter.ActiveSidecarFilter;
import org.graylog.plugins.sidecar.mapper.SidecarStatusMapper;
import org.graylog.plugins.sidecar.rest.models.NodeDetails;
import org.graylog.plugins.sidecar.rest.models.Sidecar;
import org.graylog.plugins.sidecar.rest.models.SidecarSummary;
import org.graylog.plugins.sidecar.rest.requests.RegistrationRequest;
import org.graylog.plugins.sidecar.rest.resources.SidecarResource;
import org.graylog.plugins.sidecar.rest.responses.SidecarListResponse;
import org.graylog.plugins.sidecar.services.ActionService;
import org.graylog.plugins.sidecar.services.SidecarService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.List;

import static org.graylog.plugins.sidecar.collectors.rest.assertj.ResponseAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(value = MockitoJUnitRunner.class)
public class SidecarResourceTest extends RestResourceBaseTest {
    private SidecarResource resource;
    private List<Sidecar> sidecars;

    @Mock
    private SidecarService sidecarService;

    @Mock
    private ActionService actionService;

    @Mock
    private SidecarStatusMapper statusMapper;

    @Mock
    private ClusterConfigService clusterConfigService;

    @Before
    public void setUp() throws Exception {
        this.sidecars = getDummyCollectorList();
        this.resource = new SidecarResource(
                sidecarService,
                actionService,
                clusterConfigService,
                statusMapper);
        when(sidecarService.all()).thenReturn(sidecars);
    }

    @Test
    public void testList() throws Exception {
        final SidecarListResponse response = this.resource.all();

        assertNotNull(response);
        assertNotNull(response.sidecars());
        assertEquals("Collector list should be of same size as dummy list", sidecars.size(), response.sidecars().size());
    }

    @Test(expected = NotFoundException.class)
    public void testGetNotExisting() throws Exception {
        final SidecarSummary response = this.resource.get("Nonexisting");

        assertNull(response);
    }

    @Test
    public void testGet() throws Exception {
        final Sidecar sidecar = sidecars.get(sidecars.size() - 1);
        when(sidecarService.findByNodeId(sidecar.nodeId())).thenReturn(sidecar);
        final SidecarSummary sidecarSummary = mock(SidecarSummary.class);
        when(sidecar.toSummary(any(ActiveSidecarFilter.class))).thenReturn(sidecarSummary);

        final SidecarSummary response = this.resource.get(sidecar.nodeId());

        assertNotNull(response);
        assertEquals(sidecarSummary, response);
    }

    private Sidecar getDummyCollector(String id) {
        final Sidecar sidecar = mock(Sidecar.class);
        when(sidecar.nodeId()).thenReturn(id);

        return sidecar;
    }

    private List<Sidecar> getDummyCollectorList() {
        final Sidecar sidecar1 = getDummyCollector("collector1id");
        final Sidecar sidecar2 = getDummyCollector("collector2id");
        final Sidecar sidecar3 = getDummyCollector("collector3id");

        return Lists.newArrayList(sidecar1, sidecar2, sidecar3);
    }

    @Test
    public void testRegister() throws Exception {
        final RegistrationRequest input = RegistrationRequest.create(
                "nodeName",
                NodeDetails.create(
                        "DummyOS 1.0",
                        null,
                        null,
                        null,
                        null
                )
        );

        final Response response = this.resource.register("sidecarId", input, "0.0.1");

        assertThat(response).isSuccess();
    }

    @Test
    @Ignore
    public void testRegisterInvalidCollectorId() throws Exception {
        final RegistrationRequest invalid = RegistrationRequest.create(
                "nodeName",
                NodeDetails.create(
                        "DummyOS 1.0",
                        null,
                        null,
                        null,
                        null
                )
        );

        final Response response = this.resource.register("", invalid, "0.0.1");

        assertThat(response).isError();
        assertThat(response).isStatus(Response.Status.BAD_REQUEST);
    }

    @Test
    @Ignore
    public void testRegisterInvalidNodeId() throws Exception {
        final RegistrationRequest invalid = RegistrationRequest.create(
                "",
                NodeDetails.create(
                        "DummyOS 1.0",
                        null,
                        null,
                        null,
                        null
                )
        );

        final Response response = this.resource.register("sidecarId", invalid, "0.0.1");

        assertThat(response).isError();
        assertThat(response).isStatus(Response.Status.BAD_REQUEST);
    }

    @Test
    @Ignore
    public void testRegisterMissingNodeDetails() throws Exception {
        final RegistrationRequest invalid = RegistrationRequest.create(
                "nodeName",
                null
        );

        final Response response = this.resource.register("sidecarId", invalid, "0.0.1");

        assertThat(response).isError();
        assertThat(response).isStatus(Response.Status.BAD_REQUEST);
    }

    @Test
    @Ignore
    public void testRegisterMissingOperatingSystem() throws Exception {
        final RegistrationRequest invalid = RegistrationRequest.create(
                "nodeName",
                NodeDetails.create(
                        "",
                        null,
                        null,
                        null,
                        null
                )
        );

        final Response response = this.resource.register("sidecarId", invalid, "0.0.1");

        assertThat(response).isError();
        assertThat(response).isStatus(Response.Status.BAD_REQUEST);
    }
}
