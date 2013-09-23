package lib;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
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

public class ApiClientTest {
    private static final Logger log = LoggerFactory.getLogger(ApiClientTest.class);

    private StubHttpProvider stubHttpProvider;
    private AsyncHttpClient client;

    @Test
    public void testBuildTarget() throws Exception {
        final NodeSummaryResponse r = new NodeSummaryResponse();
        r.transportAddress = "http://horst:12900";
        Node node = new Node(null, r); // TODO DI
        final URL url = ApiClient.get(EmptyResponse.class).path("/some/resource").credentials("user", "password").node(node).prepareUrl(node);
        final URL passwordWithAmpInUrl = ApiClient.get(EmptyResponse.class).path("/some/resource").credentials("user", "pass@word").node(node).prepareUrl(node);
        final URL usernameWithAmpInUrl = ApiClient.get(EmptyResponse.class).path("/some/resource").credentials("us@er", "password").node(node).prepareUrl(node);
        final URL queryParamWithPlus = ApiClient.get(EmptyResponse.class).path("/some/resource").queryParam("query", " (.+)").node(node).prepareUrl(node);

        Assert.assertEquals(url.getUserInfo(), "user:password");
        Assert.assertEquals("password should be escaped", "user:pass%40word", passwordWithAmpInUrl.getUserInfo());
        Assert.assertEquals("username should be escaped", "us%40er:password",  usernameWithAmpInUrl.getUserInfo());
        Assert.assertEquals("query param with + should be escaped", "query=%20(.%2b)",  queryParamWithPlus.getQuery());

        final URL urlWithNonAsciiChars = ApiClient.get(EmptyResponse.class).node(node).path("/some/resourçe").credentials("Sigurðsson", "password").prepareUrl(node);
        Assert.assertEquals("non-ascii chars are escaped in path", "/some/resour%C3%A7e", urlWithNonAsciiChars.getPath());
        Assert.assertEquals("non-ascii chars are escape in userinfo", "Sigur%C3%B0sson:password", urlWithNonAsciiChars.getUserInfo());
    }

    @Test
    public void testSingleExecute() throws Exception {
        final NodeSummaryResponse r = new NodeSummaryResponse();
        r.transportAddress = "http://horst:12900";
        Node node = new Node(null, r); // TODO DI

        final ApiClient.ApiRequestBuilder<EmptyResponse> requestBuilder = ApiClient.get(EmptyResponse.class).path("/some/resource").credentials("user", "password").node(node);
        stubHttpProvider.expectResponse(requestBuilder.prepareUrl(node), 200, "{}");
        final EmptyResponse response = requestBuilder.execute();

        Assert.assertNotNull(response);
        Assert.assertTrue(stubHttpProvider.isExpectationsFulfilled());
    }

    @Test
    public void testParallelExecution() throws Exception {
        final NodeSummaryResponse r = new NodeSummaryResponse();
        r.transportAddress = "http://horst1:12900";
        Node node1 = new Node(null, r); // TODO DI
        r.transportAddress = "http://horst2:12900";
        Node node2 = new Node(null, r); // TODO DI

        final ApiClient.ApiRequestBuilder<EmptyResponse> requestBuilder = ApiClient.get(EmptyResponse.class).path("/some/resource");
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
        ApiClient.setHttpClient(client);
    }

    @After
    public void tearDown() throws Exception {
        client.close();
    }
}
