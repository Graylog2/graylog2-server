package org.graylog.storage.elasticsearch7.testing;

import org.graylog.storage.elasticsearch7.ElasticsearchClient;
import org.graylog.testing.elasticsearch.FixtureImporter;

import java.net.URL;

public class FixtureImporterES7 implements FixtureImporter {
    private final ElasticsearchClient client;

    public FixtureImporterES7(ElasticsearchClient client) {
        this.client = client;
    }

    @Override
    public void importResource(URL resource) {

    }
}
