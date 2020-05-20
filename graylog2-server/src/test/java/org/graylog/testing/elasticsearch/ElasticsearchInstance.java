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
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import io.searchbox.client.JestClient;
import io.searchbox.cluster.State;
import org.graylog.testing.PropertyLoader;
import org.graylog2.bindings.providers.JestClientProvider;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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

    private static final int ES_PORT = 9200;
    private static final String NETWORK_ALIAS = "elasticsearch";

    private JestClient jestClient;
    private Client client;
    private FixtureImporter fixtureImporter = new FixtureImporter();
    private final Version version;

    public static ElasticsearchInstance create() {
        return create(Network.newNetwork());
    }

    public static ElasticsearchInstance forVersion(String version) {
        return create(version, Network.newNetwork());
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

        return new ElasticsearchInstance(image, version, network);
    }

    private static String imageNameFrom(Version version) {
        final String defaultImage = version.satisfies("^6.0.0")
                // The OSS image only exists for 6.0.0 and later
                ? DEFAULT_IMAGE_OSS
                // For older versions do not use the official image because it contains x-pack stuff we don't want
                : DEFAULT_IMAGE;
        return defaultImage + ":" + version.toString();
    }

    private ElasticsearchInstance(String image, Version version, Network network) {
        this.version = version;

        ElasticsearchContainer container = createContainer(image, version, network);

        jestClient = jestClientFrom(container);

        client = new Client(jestClient);
    }

    private ElasticsearchContainer createContainer(String image, Version version, Network network) {
        if (!containersByVersion.containsKey(version)) {
            ElasticsearchContainer container = buildContainer(image, network);
            container.start();
            containersByVersion.put(version, container);
        }
        return containersByVersion.get(version);
    }

    private ElasticsearchContainer buildContainer(String image, Network network) {
        return new ElasticsearchContainer(image)
                .withReuse(true)
                .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
                .withEnv("discovery.type", "single-node")
                .withEnv("action.auto_create_index", "false")
                .withNetwork(network)
                .withNetworkAliases(NETWORK_ALIAS)
                .waitingFor(Wait.forHttp("/").forPort(ES_PORT));
    }

    private JestClient jestClientFrom(ElasticsearchContainer container) {
        return new JestClientProvider(
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
                false,
                new ObjectMapperProvider().get()
        ).get();
    }

    @Override
    protected void after() {
        cleanUp();
    }

    public void cleanUp() {
        final State request = new State.Builder().withMetadata().build();
        final JsonNode result = JestUtils.execute(jestClient, request, () -> "failed to read state").getJsonObject();

        client.deleteTemplates(metadataFieldNamesFor(result, "templates"));
        client.deleteIndices(metadataFieldNamesFor(result, "indices"));
    }

    private String[] metadataFieldNamesFor(JsonNode result, String templates) {
        return toArray(result.get("metadata").get(templates).fieldNames(), String.class);
    }

    public static String internalUri() {
        return String.format(Locale.US, "http://%s:%d", NETWORK_ALIAS, ES_PORT);
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

    public void importFixtureResource(String resourcePath, Class<?> testClass) {
        boolean isFullResourcePath = Paths.get(resourcePath).getNameCount() > 1;

        @SuppressWarnings("UnstableApiUsage") final URL fixtureResource = isFullResourcePath
                ? Resources.getResource(resourcePath)
                : Resources.getResource(testClass, resourcePath);

        fixtureImporter.importResource(fixtureResource, jestClient);

        // Make sure the data we just imported is visible
        client().refreshNode();
    }
}
