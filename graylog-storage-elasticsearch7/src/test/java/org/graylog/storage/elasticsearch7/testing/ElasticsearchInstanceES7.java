package org.graylog.storage.elasticsearch7.testing;

import com.github.joschi.jadconfig.util.Duration;
import com.github.zafarkhaja.semver.Version;
import com.google.common.collect.ImmutableList;
import org.graylog.storage.elasticsearch7.ElasticsearchClient;
import org.graylog.testing.elasticsearch.Client;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.graylog.testing.elasticsearch.FixtureImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.net.URI;

public class ElasticsearchInstanceES7 extends ElasticsearchInstance {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchInstanceES7.class);
    private static final String DEFAULT_VERSION = "7.8.0";

    private final ElasticsearchClient elasticsearchClient;
    private final Client client;
    private final FixtureImporter fixtureImporter;

    protected ElasticsearchInstanceES7(String image, Version version, Network network) {
        super(image, version, network);
        this.elasticsearchClient = elasticsearchClientFrom(this.container);
        this.client = new ClientES7(this.elasticsearchClient);
        this.fixtureImporter = new FixtureImporterES7(this.elasticsearchClient);
    }

    private ElasticsearchClient elasticsearchClientFrom(ElasticsearchContainer container) {
        return new ElasticsearchClient(
                ImmutableList.of(URI.create("http://" + container.getHttpHostAddress())),
                Duration.seconds(60),
                Duration.seconds(60),
                Duration.seconds(60),
                1,
                1,
                1,
                false,
                null,
                Duration.seconds(60),
                "http",
                false
        );
    }

    public static ElasticsearchInstanceES7 create() {
        return create(Network.newNetwork());
    }

    public static ElasticsearchInstanceES7 create(Network network) {
        return create(DEFAULT_VERSION, network);
    }

    public static ElasticsearchInstanceES7 create(String versionString, Network network) {
        final Version version = Version.valueOf(versionString);
        final String image = imageNameFrom(version);

        LOG.debug("Creating instance {}", image);

        return new ElasticsearchInstanceES7(image, version, network);
    }

    @Override
    protected Client client() {
        return this.client;
    }

    @Override
    protected FixtureImporter fixtureImporter() {
        return this.fixtureImporter;
    }

    public ElasticsearchClient elasticsearchClient() {
        return this.elasticsearchClient;
    }
}
