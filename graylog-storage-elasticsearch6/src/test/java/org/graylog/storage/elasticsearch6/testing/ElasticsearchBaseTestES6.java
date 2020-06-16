package org.graylog.storage.elasticsearch6.testing;

import org.graylog.testing.elasticsearch.Client;
import org.graylog.testing.elasticsearch.ElasticsearchBaseTest;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.junit.Rule;

public class ElasticsearchBaseTestES6 extends ElasticsearchBaseTest {
    @Rule
    public final ElasticsearchInstance elasticsearch = ElasticsearchInstanceES6.create();

    @Override
    protected ElasticsearchInstance elasticsearch() {
        return this.elasticsearch;
    }

    @Override
    protected Client client() {
        return null;
    }
}
