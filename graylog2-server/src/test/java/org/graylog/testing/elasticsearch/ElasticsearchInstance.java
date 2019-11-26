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

import com.fasterxml.jackson.databind.JsonNode;
import com.github.joschi.jadconfig.util.Duration;
import com.github.zafarkhaja.semver.Version;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import io.searchbox.client.JestClient;
import io.searchbox.cluster.State;
import org.graylog.testing.PropertyLoader;
import org.graylog2.bindings.providers.JestClientProvider;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Iterators.toArray;

/**
 * This rule starts an Elasticsearch instance and provides a configured {@link JestClient}.
 */
public class ElasticsearchInstance extends ExternalResource {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchInstance.class);

    private static final Map<Version, ElasticsearchContainer> containersByVersion = new HashMap<>();

    private static final String PROPERTIES_RESOURCE_NAME = "elasticsearch.properties";

    private static final String DEFAULT_IMAGE_OSS = "docker.elastic.co/elasticsearch/elasticsearch-oss";
    private static final String DEFAULT_IMAGE = "elasticsearch";
    private static final String DEFAULT_VERSION = "6.8.4";

    private final ElasticsearchContainer container;
    private JestClient jestClient;
    private Client client;
    private FixtureImporter fixtureImporter = new FixtureImporter();
    private final Version version;

    public static ElasticsearchInstance create() {
        final Properties properties = PropertyLoader.loadProperties(PROPERTIES_RESOURCE_NAME);

        return create(properties.getProperty("version", DEFAULT_VERSION));
    }

    public static ElasticsearchInstance create(String versionString) {
        final Version version = Version.valueOf(versionString);
        final String image = imageNameFrom(version);

        LOG.debug("Creating instance {}", image);

        return new ElasticsearchInstance(image, version);
    }

    private static String imageNameFrom(Version version) {
        final String defaultImage = version.satisfies("^6.0.0")
                // The OSS image only exists for 6.0.0 and later
                ? DEFAULT_IMAGE_OSS
                // For older versions do not use the official image because it contains x-pack stuff we don't want
                : DEFAULT_IMAGE;
        return defaultImage + ":" + version.toString();
    }

    private ElasticsearchInstance(String image, Version version) {
        this.version = version;
        this.container = createContainer(image, version);
    }

    private static ElasticsearchContainer createContainer(String image, Version version) {
        if (!containersByVersion.containsKey(version)) {
            containersByVersion.put(version, startNewContainerInstance(image));
        }
        return containersByVersion.get(version);
    }

    private static ElasticsearchContainer startNewContainerInstance(String image) {
        final Stopwatch sw = Stopwatch.createStarted();

        final ElasticsearchContainer container = new ElasticsearchContainer(image)
                .withReuse(true)
                .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
                .withEnv("discovery.type", "single-node")
                .withEnv("action.auto_create_index", "false")
                .waitingFor(Wait.forHttp("/").forPort(9200));
        container.start();
        LOG.debug("Started container {} in {}ms", container.getContainerInfo().getName(), sw.elapsed(TimeUnit.MILLISECONDS));
        return container;
    }

    @Override
    protected void before() {
        jestClient = new JestClientProvider(
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
                false,
                new ObjectMapperProvider().get()
        ).get();

        client = new Client(jestClient);
    }

    @Override
    protected void after() {
        cleanUp();
    }

    private void cleanUp() {
        final State request = new State.Builder().withMetadata().build();
        final JsonNode result = JestUtils.execute(jestClient, request, () -> "failed to read state").getJsonObject();

        client.deleteTemplates(metadataFieldNamesFor(result, "templates"));
        client.deleteIndices(metadataFieldNamesFor(result, "indices"));
    }

    private String[] metadataFieldNamesFor(JsonNode result, String templates) {
        return toArray(result.get("metadata").get(templates).fieldNames(), String.class);
    }

    public Version version() {
        return version;
    }

    public JestClient jestClient() {
        return jestClient;
    }

    public Client client() {
        return client;
    }

    FixtureImporter fixtureImporter() {
        return fixtureImporter;
    }
}
