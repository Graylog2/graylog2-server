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

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import models.ModelFactoryModule;
import models.Node;
import models.api.responses.EmptyResponse;
import models.api.responses.NodeSummaryResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ApiClientTest {
    private static final Logger log = LoggerFactory.getLogger(ApiClientTest.class);

    private StubHttpProvider stubHttpProvider;
    private AsyncHttpClient client;

    public Injector setup(final Node[] initialNodes) {
        List<Module> modules = Lists.newArrayList();
        modules.add(new ModelFactoryModule());

        modules.add(new AbstractModule() {
            @Override
            protected void configure() {
                bind(Node[].class).annotatedWith(Names.named("Initial Nodes")).toInstance(initialNodes);
            }
        });
        return Guice.createInjector(modules);
    }

    @Test
    public void testBuildTarget() throws Exception {
        final NodeSummaryResponse r = new NodeSummaryResponse();
        r.transportAddress = "http://horst:12900";
        final Node node = new Node(r); // can't use injector here... we need to have the initial list :(

        final Injector injector = setup(new Node[] {node});
        final ApiClient api = injector.getInstance(ApiClient.class);
        api.setHttpClient(client);

        final URL url = api.get(EmptyResponse.class).path("/some/resource").credentials("user", "password").node(node).prepareUrl(node);
        final URL passwordWithAmpInUrl = api.get(EmptyResponse.class).path("/some/resource").credentials("user", "pass@word").node(node).prepareUrl(node);
        final URL usernameWithAmpInUrl = api.get(EmptyResponse.class).path("/some/resource").credentials("us@er", "password").node(node).prepareUrl(node);
        final URL queryParamWithPlus = api.get(EmptyResponse.class).path("/some/resource").queryParam("query", " (.+)").node(node).prepareUrl(node);

        Assert.assertEquals(url.getUserInfo(), "user:password");
        Assert.assertEquals("password should be escaped", "user:pass%40word", passwordWithAmpInUrl.getUserInfo());
        Assert.assertEquals("username should be escaped", "us%40er:password", usernameWithAmpInUrl.getUserInfo());
        Assert.assertEquals("query param with + should be escaped", "query=%20(.%2b)", queryParamWithPlus.getQuery());

        final URL urlWithNonAsciiChars = api.get(EmptyResponse.class).node(node).path("/some/resourçe").credentials("Sigurðsson", "password").prepareUrl(node);
        Assert.assertEquals("non-ascii chars are escaped in path", "/some/resour%C3%A7e", urlWithNonAsciiChars.getPath());
        Assert.assertEquals("non-ascii chars are escape in userinfo", "Sigur%C3%B0sson:password", urlWithNonAsciiChars.getUserInfo());
    }

    @Test
    public void testSingleExecute() throws Exception {
        final NodeSummaryResponse r = new NodeSummaryResponse();
        r.transportAddress = "http://horst:12900";
        final Node node = new Node(r); // can't use injector here... we need to have the initial list :(

        final Injector injector = setup(new Node[] {node});
        final ApiClient api = injector.getInstance(ApiClient.class);
        api.setHttpClient(client);

        final ApiClient.ApiRequestBuilder<EmptyResponse> requestBuilder =
                api.get(EmptyResponse.class)
                        .path("/some/resource")
                        .credentials("user", "password")
                        .node(node)
                        .timeout(1, TimeUnit.SECONDS);
        stubHttpProvider.expectResponse(requestBuilder.prepareUrl(node), 200, "{}");
        final EmptyResponse response = requestBuilder.execute();

        Assert.assertNotNull(response);
        Assert.assertTrue(stubHttpProvider.isExpectationsFulfilled());
    }

    @Test
    public void testParallelExecution() throws Exception {
        final NodeSummaryResponse r = new NodeSummaryResponse();
        r.transportAddress = "http://horst1:12900";
        Node node1 = new Node(r); // TODO DI
        r.transportAddress = "http://horst2:12900";
        Node node2 = new Node(r); // TODO DI

        final Injector injector = setup(new Node[] {node1, node2});
        final ApiClient api = injector.getInstance(ApiClient.class);
        api.setHttpClient(client);

        final ApiClient.ApiRequestBuilder<EmptyResponse> requestBuilder = api.get(EmptyResponse.class).path("/some/resource");
        final URL url1 = requestBuilder.prepareUrl(node1);
        final URL url2 = requestBuilder.prepareUrl(node2);
        stubHttpProvider.expectResponse(url1, 200, "{}");
        stubHttpProvider.expectResponse(url2, 200, "{}");

        final Collection<EmptyResponse> responses = requestBuilder.nodes(node1, node2).executeOnAll();
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
