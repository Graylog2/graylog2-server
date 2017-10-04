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
package org.graylog2.rest.resources;

import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterId;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.rest.models.HelloWorldResponse;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HelloWorldResourceTest extends RestResourceBaseTest {
    private static final String CK_CLUSTER_ID = "dummyclusterid";
    private static final String CK_NODE_ID = "dummynodeid";

    private HelloWorldResource helloWorldResource;
    private NodeId nodeId;
    private ClusterConfigService clusterConfigService;

    @Before
    public void setUp() throws Exception {
        this.nodeId = mock(NodeId.class);
        this.clusterConfigService = mock(ClusterConfigService.class);
        this.helloWorldResource = new HelloWorldResource(nodeId, clusterConfigService);

        when(clusterConfigService.getOrDefault(eq(ClusterId.class), any(ClusterId.class))).thenReturn(ClusterId.create(CK_CLUSTER_ID));
        when(nodeId.toString()).thenReturn(CK_NODE_ID);
    }

    @Test
    public void rootResourceShouldReturnGeneralStats() throws Exception {
        final HelloWorldResponse helloWorldResponse = this.helloWorldResource.helloWorld();

        assertThat(helloWorldResponse).isNotNull();

        assertThat(helloWorldResponse.clusterId()).isEqualTo(CK_CLUSTER_ID);
        assertThat(helloWorldResponse.nodeId()).isEqualTo(CK_NODE_ID);
    }

    @Test
    public void rootResourceShouldRedirectToWebInterfaceIfHtmlIsRequested() throws Exception {
        final Response response = helloWorldResource.redirectToWebConsole();

        assertThat(response).isNotNull();

        final String locationHeader = response.getHeaderString("Location");
        assertThat(locationHeader).isNotNull().isEqualTo(HttpConfiguration.PATH_WEB);
    }
}
