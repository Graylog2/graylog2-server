package org.graylog.storage.elasticsearch6.testing;

import io.searchbox.client.JestClient;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;

public class TestUtils {
    public static JestClient jestClient(ElasticsearchInstance elasticsearchInstance) {
        if (elasticsearchInstance instanceof ElasticsearchInstanceES6) {
            return ((ElasticsearchInstanceES6) elasticsearchInstance).jestClient();
        }

        throw new RuntimeException("Unable to return Jest client, Elasticsearch instance is of wrong type: " + elasticsearchInstance);
    }
}
