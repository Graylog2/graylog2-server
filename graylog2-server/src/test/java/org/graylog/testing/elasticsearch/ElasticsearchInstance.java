/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.testing.elasticsearch;

import com.github.joschi.jadconfig.util.Duration;
import com.github.zafarkhaja.semver.Version;
import com.google.common.collect.ImmutableList;
import io.searchbox.client.JestClient;
import org.graylog.testing.PropertyLoader;
import org.graylog2.bindings.providers.JestClientProvider;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.net.URI;
import java.util.Properties;

/**
 * This rule starts an Elasticsearch instance and provides a configured {@link JestClient}.
 */
public class ElasticsearchInstance extends ExternalResource {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchInstance.class);

    private static final String PROPERTIES_RESOURCE_NAME = "elasticsearch.properties";

    private static final String DEFAULT_IMAGE_OSS = "docker.elastic.co/elasticsearch/elasticsearch-oss";
    private static final String DEFAULT_IMAGE = "elasticsearch";
    private static final String DEFAULT_VERSION = "6.5.2";

    private final Network network;
    private final ElasticsearchContainer container;
    private JestClient jestClient;
    private FixtureImporter fixtureImporter = new FixtureImporter();
    private final Version elasticsearchVersion;

    public static ElasticsearchInstance create() {
        final Properties properties = PropertyLoader.loadProperties(PROPERTIES_RESOURCE_NAME);

        return create(properties.getProperty("version", DEFAULT_VERSION));
    }

    public static ElasticsearchInstance create(String elasticsearchVersion) {
        final Version version = Version.valueOf(elasticsearchVersion);
        final String image;

        if (version.satisfies("^6.0.0")) {
            // The OSS image only exists for 6.0.0 and later
            image = DEFAULT_IMAGE_OSS + ":" + version.toString();
        } else {
            // For older versions do not use the official image because it contains x-pack stuff we don't want
            image = DEFAULT_IMAGE + ":" + version.toString();
        }

        LOG.info("Creating instance {}", image);

        return new ElasticsearchInstance(image, version);
    }

    private ElasticsearchInstance(String image, Version elasticsearchVersion) {
        this.elasticsearchVersion = elasticsearchVersion;
        this.network = Network.newNetwork();
        this.container = new ElasticsearchContainer(image)
                .withNetwork(network)
                .waitingFor(Wait.forHttp("/").forPort(9200));
    }

    @Override
    protected void before() {
        container.start();

        LOG.info("Started container {}{}", container.getContainerInfo().getId(), container.getContainerInfo().getName());

        jestClient = new JestClientProvider(
                ImmutableList.of(URI.create("http://"+ container.getHttpHostAddress())),
                Duration.seconds(60),
                Duration.seconds(60),
                Duration.seconds(60),
                1,
                1,
                1,
                false,
                null,
                Duration.seconds(60),
                false,
                new ObjectMapperProvider().get()
        ).get();
    }

    @Override
    protected void after() {
        LOG.info("Stopping container {}{}", container.getContainerInfo().getId(), container.getContainerInfo().getName());
        container.stop();
        try {
            network.close();
        } catch (Exception e) {
            throw new RuntimeException("Couldn't close container network", e);
        }
    }

    public Version version() {
        return elasticsearchVersion;
    }

    public JestClient client() {
        return jestClient;
    }

    public FixtureImporter fixtureImporter() {
        return fixtureImporter;
    }
}
