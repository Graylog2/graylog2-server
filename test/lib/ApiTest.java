package lib;

import models.Node;
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
        final URL url = Api.buildTarget(node, "/some/resource", "user", "password");
        final URL passwordWithAmpInUrl = Api.buildTarget(node, "/some/resource", "user", "pass@word");
        final URL usernameWithAmpInUrl = Api.buildTarget(node, "/some/resource", "us@er", "password");

        Assert.assertEquals(url.getUserInfo(), "user:password");
        Assert.assertEquals("password should be escaped", "user:pass%40word", passwordWithAmpInUrl.getUserInfo());
        Assert.assertEquals("username should be escaped", "us%40er:password",  usernameWithAmpInUrl.getUserInfo());

        final URL urlWithNonAsciiChars = Api.buildTarget(node, "/some/resourçe", "Sigurðsson", "password");
        Assert.assertEquals("non-ascii chars are escaped in path", "/some/resour%C3%A7e", urlWithNonAsciiChars.getPath());
        Assert.assertEquals("non-ascii chars are escape in userinfo", "Sigur%C3%B0sson:password", urlWithNonAsciiChars.getUserInfo());
    }
}
