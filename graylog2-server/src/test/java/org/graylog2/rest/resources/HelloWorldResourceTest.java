package org.graylog2.rest.resources;

import org.graylog2.Configuration;
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
    private Configuration configuration;

    @Before
    public void setUp() throws Exception {
        this.nodeId = mock(NodeId.class);
        this.clusterConfigService = mock(ClusterConfigService.class);
        this.configuration = mock(Configuration.class);
        this.helloWorldResource = new HelloWorldResource(nodeId, clusterConfigService, configuration);

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
        when(configuration.isRestAndWebOnSamePort()).thenReturn(true);
        final String pathToWebIf = "/path_to_web_if";
        when(configuration.getWebPrefix()).thenReturn(pathToWebIf);

        final Response response = helloWorldResource.redirectToWebConsole();

        assertThat(response).isNotNull();

        final String locationHeader = response.getHeaderString("Location");
        assertThat(locationHeader).isNotNull().isEqualTo(pathToWebIf);
    }

    @Test
    public void rootResourceShouldNotRedirectToWebInterfaceIfNotRunningOnSamePort() throws Exception {
        when(configuration.isRestAndWebOnSamePort()).thenReturn(false);

        final Response response = helloWorldResource.redirectToWebConsole();

        assertThat(response).isNotNull();

        final String locationHeader = response.getHeaderString("Location");
        assertThat(locationHeader).isNull();
    }
}
