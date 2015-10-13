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

import com.github.joschi.jadconfig.util.Duration;
import com.google.common.collect.Lists;
import org.graylog2.collectors.Collector;
import org.graylog2.collectors.CollectorNodeDetails;
import org.graylog2.collectors.CollectorService;
import org.graylog2.rest.models.collector.CollectorNodeDetailsSummary;
import org.graylog2.rest.models.collector.requests.CollectorRegistrationRequest;
import org.graylog2.rest.models.collector.responses.CollectorList;
import org.graylog2.rest.models.collector.responses.CollectorSummary;
import org.graylog2.rest.resources.RestResourceBaseTest;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.List;

import static org.graylog2.rest.assertj.ResponseAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(value = MockitoJUnitRunner.class)
public class CollectorResourceTest extends RestResourceBaseTest {
    private CollectorResource resource;
    private List<Collector> collectors;

    @Mock
    private CollectorService collectorService;

    @Before
    public void setUp() throws Exception {
        this.collectors = getDummyCollectorList();
        this.resource = new CollectorResource(collectorService, Duration.minutes(1));
        when(collectorService.all()).thenReturn(collectors);
    }

    @Test
    public void testList() throws Exception {
        final CollectorList response = this.resource.list();

        assertNotNull(response);
        assertNotNull(response.collectors());
        assertEquals("Collector list should be of same size as dummy list", collectors.size(), response.collectors().size());
    }

    @Test(expected = NotFoundException.class)
    public void testGetNotExisting() throws Exception {
        final CollectorSummary response = this.resource.get("Nonexisting");

        assertNull(response);
    }

    @Test
    public void testGet() throws Exception {
        final Collector collector = collectors.get(collectors.size() - 1);
        when(collectorService.findById(collector.getId())).thenReturn(collector);
        final CollectorSummary collectorSummary = mock(CollectorSummary.class);
        when(collector.toSummary(any(CollectorResource.LostCollectorFunction.class))).thenReturn(collectorSummary);

        final CollectorSummary response = this.resource.get(collector.getId());

        assertNotNull(response);
        assertEquals(collectorSummary, response);
    }

    private Collector getDummyCollector(String id, String nodeId, DateTime lastSeen, String operatingSystem) {
        final CollectorNodeDetails collectorNodeDetails = mock(CollectorNodeDetails.class);
        when(collectorNodeDetails.operatingSystem()).thenReturn(operatingSystem);

        final Collector collector = mock(Collector.class);
        when(collector.getId()).thenReturn(id);
        when(collector.getNodeId()).thenReturn(nodeId);
        when(collector.getLastSeen()).thenReturn(lastSeen);
        when(collector.getNodeDetails()).thenReturn(collectorNodeDetails);
        when(collector.getNodeDetails().operatingSystem()).thenReturn(operatingSystem);

        return collector;
    }

    private List<Collector> getDummyCollectorList() {
        final Collector collector1 = getDummyCollector("collector1id", "collector1nodeid", DateTime.now(DateTimeZone.UTC), "DummyOS 1.0");
        final Collector collector2 = getDummyCollector("collector2id", "collector2nodeid", DateTime.now(DateTimeZone.UTC), "DummyOS 1.0");
        final Collector collector3 = getDummyCollector("collector3id", "collector3nodeid", DateTime.now(DateTimeZone.UTC), "DummyOS 1.0");

        return Lists.newArrayList(collector1, collector2, collector3);
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
