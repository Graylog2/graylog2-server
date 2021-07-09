package org.graylog.storage;

import org.apache.commons.lang3.NotImplementedException;
import org.graylog.storage.elasticsearch6.testing.ElasticsearchInstanceES6;
import org.graylog.storage.elasticsearch7.testing.ElasticsearchInstanceES7;
import org.graylog.testing.completebackend.ElasticsearchInstanceFactory;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.testcontainers.containers.Network;

public class ElasticSearchInstanceFactoryByVersion implements ElasticsearchInstanceFactory {
    private String version;

    @Override
    public ElasticsearchInstance create(Network network) {
        throw new NotImplementedException("Create without version not implemented with this factory.");
    }

    @Override
    public ElasticsearchInstance create(String version, Network network) {
        this.version = version;
        if("6".equals(version())) {
            return ElasticsearchInstanceES6.create(version, network);
        } else if("7".equals(version())) {
            return ElasticsearchInstanceES7.create(version, network);
        } else {
            throw new NotImplementedException("ES version " + version + " not supported.");
        }
    }

    @Override
    public String version() {
        return version.split("\\.")[0];
    }
}
