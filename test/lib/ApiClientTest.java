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

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import org.graylog2.restclient.lib.ApiRequestBuilder;
import org.graylog2.restclient.models.Node;
import org.graylog2.restclient.models.api.responses.EmptyResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ApiClientTest extends BaseApiTest {
    private static final Logger log = LoggerFactory.getLogger(ApiClientTest.class);

    private StubHttpProvider stubHttpProvider;
    private AsyncHttpClient client;

    @Test
    public void testBuildTarget() throws Exception {
        setupNodes(AddressNodeId.create("http://horst:12900"));

        // we only have one node configured here
        final Node node = serverNodes.any();
        api.setHttpClient(client);

        final URL url = api.get(EmptyResponse.class).path("/some/resource").session("foo").node(node).prepareUrl(node);
        final URL queryParamWithPlus = api.get(EmptyResponse.class).path("/some/resource").queryParam("query", " (.+)").node(node).unauthenticated().prepareUrl(node);

        Assert.assertEquals(url.getUserInfo(), "foo:session");
        Assert.assertEquals("query param with + should be escaped", "query=+(.%2B)", queryParamWithPlus.getQuery());

        final URL queryParamWithDoubleQuotes = api.get(EmptyResponse.class).path("/some/resource").queryParam("query", " \".+\"").node(node).unauthenticated().prepareUrl(node);
        Assert.assertEquals("query param with \" should be escaped", "query=+%22.%2B%22", queryParamWithDoubleQuotes.getQuery());
        
        final URL urlWithNonAsciiChars = api.get(EmptyResponse.class).node(node).path("/some/resourçe").unauthenticated().prepareUrl(node);
        Assert.assertEquals("non-ascii chars are escaped in path", "/some/resour%C3%A7e", urlWithNonAsciiChars.getPath());

        final URL queryWithAmp = api.get(EmptyResponse.class).node(node).path("/").queryParam("foo", "this&that").prepareUrl(node);
        Assert.assertEquals("Query params are escaped", "foo=this%26that", queryWithAmp.getQuery());
    }

    @Test
    public void testSingleExecute() throws Exception {
        setupNodes(AddressNodeId.create("http://horst:12900"));

        // we only have one node configured here
        final Node node = serverNodes.any();
        api.setHttpClient(client);

        final ApiRequestBuilder<EmptyResponse> requestBuilder =
                api.get(EmptyResponse.class)
                        .path("/some/resource")
                        .unauthenticated()
                        .node(node)
                        .timeout(1, TimeUnit.SECONDS);
        stubHttpProvider.expectResponse(requestBuilder.prepareUrl(node), 200, "{}");
        final EmptyResponse response = requestBuilder.execute();

        Assert.assertNotNull(response);
        Assert.assertTrue(stubHttpProvider.isExpectationsFulfilled());
    }

    @Test
    public void testParallelExecution() throws Exception {
        setupNodes(AddressNodeId.create("http://horst1:12900"), AddressNodeId.create("http://horst2:12900"));

        final Collection<Node> nodes = serverNodes.all();
        final Iterator<Node> it = nodes.iterator();
        Node node1 = it.next();
        Node node2 = it.next();
        api.setHttpClient(client);

        final ApiRequestBuilder<EmptyResponse> requestBuilder = api.get(EmptyResponse.class).path("/some/resource");
        final URL url1 = requestBuilder.prepareUrl(node1);
        final URL url2 = requestBuilder.prepareUrl(node2);
        stubHttpProvider.expectResponse(url1, 200, "{}");
        stubHttpProvider.expectResponse(url2, 200, "{}");

        final Map<Node, EmptyResponse> responses = requestBuilder.nodes(node1, node2).executeOnAll();
        Assert.assertFalse(responses.isEmpty());
        Assert.assertTrue(stubHttpProvider.isExpectationsFulfilled());
    }

    @Before
    public void setUp() throws Exception {
        AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();
        builder.setAllowPoolingConnection(false);
        stubHttpProvider = new StubHttpProvider();
        client = new AsyncHttpClient(stubHttpProvider, builder.build());
    }

    @After
    public void tearDown() throws Exception {
        client.close();
        client = null;
    }

}
