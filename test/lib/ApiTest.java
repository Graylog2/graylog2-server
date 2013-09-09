package lib;

import models.Node;
import models.api.responses.EmptyResponse;
import models.api.responses.NodeSummaryResponse;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;

public class ApiTest {

    @Test
    public void testBuildTarget() throws Exception {
        final NodeSummaryResponse r = new NodeSummaryResponse();
        r.transportAddress = "http://horst:12900";
        Node node = new Node(r);
        final URL url = ApiClient.get(EmptyResponse.class).path("/some/resource").credentials("user", "password").node(node).prepareUrl();
        final URL passwordWithAmpInUrl = ApiClient.get(EmptyResponse.class).path("/some/resource").credentials("user", "pass@word").node(node).prepareUrl();
        final URL usernameWithAmpInUrl = ApiClient.get(EmptyResponse.class).path("/some/resource").credentials("us@er", "password").node(node).prepareUrl();
        final URL queryParamWithPlus = ApiClient.get(EmptyResponse.class).path("/some/resource").queryParam("query", " (.+)").node(node).prepareUrl();

        Assert.assertEquals(url.getUserInfo(), "user:password");
        Assert.assertEquals("password should be escaped", "user:pass%40word", passwordWithAmpInUrl.getUserInfo());
        Assert.assertEquals("username should be escaped", "us%40er:password",  usernameWithAmpInUrl.getUserInfo());
        Assert.assertEquals("query param with + should be escaped", "query=%20(.%2b)",  queryParamWithPlus.getQuery());

        final URL urlWithNonAsciiChars = ApiClient.get(EmptyResponse.class).node(node).path("/some/resourçe").credentials("Sigurðsson", "password").prepareUrl();
        Assert.assertEquals("non-ascii chars are escaped in path", "/some/resour%C3%A7e", urlWithNonAsciiChars.getPath());
        Assert.assertEquals("non-ascii chars are escape in userinfo", "Sigur%C3%B0sson:password", urlWithNonAsciiChars.getUserInfo());
    }
}
