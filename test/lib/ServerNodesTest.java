/*
 * Copyright 2013 TORCH UG
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package lib;

import com.google.common.collect.ImmutableList;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import org.graylog2.restclient.lib.Graylog2ServerUnavailableException;
import org.graylog2.restclient.models.Node;
import org.graylog2.restclient.models.api.responses.EmptyResponse;
import org.graylog2.restclient.models.api.responses.cluster.NodeSummaryResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

public class ServerNodesTest extends BaseApiTest {

    private AsyncHttpClient client;

    @Test
    public void testTransportAddressPathNormalization() {
        setupNodes(AddressNodeId.create("http://localhost:65535"));
        api.setHttpClient(client);
        final Node node = serverNodes.any();
        assertEquals("transport address should have no path", "http://localhost:65535", node.getTransportAddress());
    }

    @Test
    public void testFailureCountSingleExecute() throws Exception {
        setupNodes(AddressNodeId.create("http://localhost:65535"));
        api.setHttpClient(client);

        final Node node = serverNodes.any();

        try {
            api.put().path("/").node(node).execute();
        } catch (Graylog2ServerUnavailableException e) {
            Node failedNode = serverNodes.any(true);
            assertFalse("Node should be from configuration", failedNode.isActive());
            assertEquals(1, failedNode.getFailureCount());
        }
    }

    @Test
    public void testFailureCountParallelExecute() throws Exception {
        setupNodes(AddressNodeId.create("http://localhost:65535"),AddressNodeId.create("http://localhost:65534"));
        api.setHttpClient(client);

        final Map<Node, EmptyResponse> emptyResponses = api.put().path("/").executeOnAll();

        assertTrue("Request should have failed", emptyResponses.isEmpty());
        final List<Node> nodes = serverNodes.all(true);
        Node failedNode = nodes.get(0);
        Node failedNode2 = nodes.get(1);
        assertFalse("Node should be inactive" , failedNode.isActive());
        assertFalse("Node should be inactive", failedNode2.isActive());
        assertEquals(1, failedNode.getFailureCount());
        assertEquals(1, failedNode2.getFailureCount());
    }

    @Test
    public void testNodeObjectsRememberedByAddress() throws Exception {
        final AddressNodeId addressNodeId = AddressNodeId.create("http://localhost:65535", UUID.randomUUID().toString());
        setupNodes(addressNodeId);

        api.setHttpClient(client);
        final Node firstNode = serverNodes.any();

        Throwable t = null;
        try {
            api.put().path("/").node(firstNode).execute();
        } catch (Graylog2ServerUnavailableException e) {
            t = e;
        }
        assertNotNull("Should have thrown an Graylog2ServerUnavailableException", t);
        assertEquals("First node failure count should be 1", 1, firstNode.getFailureCount());

        final Node.Factory nodeFactory = injector.getInstance(Node.Factory.class);
        final NodeSummaryResponse r1 = new NodeSummaryResponse();
        r1.transportAddress = "http://localhost:65534";
        r1.nodeId = UUID.randomUUID().toString();

        final Node newNode = nodeFactory.fromSummaryResponse(r1);
        newNode.touch();
        final NodeSummaryResponse r2 = new NodeSummaryResponse();
        r2.transportAddress = firstNode.getTransportAddress();
        r2.nodeId = firstNode.getNodeId();
        final Node sameAsInitialNode = nodeFactory.fromSummaryResponse(r2);
        sameAsInitialNode.touch();
        serverNodes.put(ImmutableList.of(newNode, sameAsInitialNode));

        final Map<Node, EmptyResponse> responses = api.put().nodes(serverNodes.all().toArray(new Node[0])).path("/").executeOnAll();

        assertTrue(responses.isEmpty());
        assertEquals("new node's failureCount is 1", 1, newNode.getFailureCount());
        assertEquals("initial node's failureCount is 2", 2, firstNode.getFailureCount());

    }

    @Before
    public void setUp() throws Exception {
        AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();
        builder.setAllowPoolingConnection(false);
        client = new AsyncHttpClient(builder.build());
    }

    @After
    public void tearDown() throws Exception {
        client.close();
        client = null;
    }
}
