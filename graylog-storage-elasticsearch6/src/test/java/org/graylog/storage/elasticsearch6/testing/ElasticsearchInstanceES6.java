package org.graylog.storage.elasticsearch6.testing;

import com.github.zafarkhaja.semver.Version;
import org.graylog.testing.PropertyLoader;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;

public class ElasticsearchInstanceES6 extends ElasticsearchInstance {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchInstance.class);
    private static final String DEFAULT_VERSION = "6.8.4";
    private static final String PROPERTIES_RESOURCE_NAME = "elasticsearch.properties";

    public ElasticsearchInstanceES6(String image, Version version, Network network) {
        super(image, version, network);
    }

    public static ElasticsearchInstance create() {
        return create(Network.newNetwork());
    }

    public static ElasticsearchInstance create(Network network) {
        String version = PropertyLoader
                .loadProperties(PROPERTIES_RESOURCE_NAME)
                .getProperty("version", DEFAULT_VERSION);

        return create(version, network);
    }

    public static ElasticsearchInstance create(String versionString, Network network) {
        final Version version = Version.valueOf(versionString);
        final String image = imageNameFrom(version);

        LOG.debug("Creating instance {}", image);

        return new ElasticsearchInstanceES6(image, version, network);
    }
}
