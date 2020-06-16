package org.graylog.storage.elasticsearch6.testing;

import com.github.zafarkhaja.semver.Version;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.testcontainers.containers.Network;

public class ElasticsearchInstanceES6 extends ElasticsearchInstance {
    private static final Version CURRENT_VERSION = Version.forIntegers(5, 6, 12);
    public ElasticsearchInstanceES6(Network network) {
        super(imageNameFrom(CURRENT_VERSION), CURRENT_VERSION, network);
    }
}
