package org.graylog.testing.elasticsearch.multiversion;

import io.searchbox.action.GenericResultAbstractAction;
import io.searchbox.client.JestResult;
import org.graylog.testing.elasticsearch.Client;

public class FetchVersionUtil {
    static String fetchVersion(Client client) {
        JestResult result = client.executeWithExpectedSuccess(new GetStartPage(), "");

        return result.getJsonObject().at("/version/number").asText();
    }

    private static class GetStartPage extends GenericResultAbstractAction {
        public GetStartPage() {
            setURI(buildURI());
        }

        @Override
        public String getRestMethodName() {
            return "GET";
        }
    }
}
