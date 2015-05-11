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
import org.graylog2.Configuration;
import org.graylog2.collectors.Collector;
import org.graylog2.collectors.CollectorNodeDetails;
import org.graylog2.collectors.CollectorService;
import org.graylog2.database.NotFoundException;
import org.graylog2.rest.models.collector.responses.CollectorList;
import org.graylog2.rest.models.collector.responses.CollectorSummary;
import org.graylog2.rest.resources.RestResourceBaseTest;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.*;
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
        final Configuration config = mock(Configuration.class);
        when(config.getCollectorInactiveThreshold()).thenReturn(Duration.minutes(1));
        this.resource = new CollectorResource(collectorService, config);
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
        final Collector collector1 = getDummyCollector("collector1id", "collector1nodeid", DateTime.now(), "DummyOS 1.0");
        final Collector collector2 = getDummyCollector("collector2id", "collector2nodeid", DateTime.now(), "DummyOS 1.0");
        final Collector collector3 = getDummyCollector("collector3id", "collector3nodeid", DateTime.now(), "DummyOS 1.0");

        return Lists.newArrayList(collector1, collector2, collector3);
    }
}