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

import com.github.zafarkhaja.semver.Version;
import com.google.common.io.Resources;
import org.junit.rules.ExternalResource;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * This rule starts an Elasticsearch instance and provides a configured {@link Client}.
 */
public abstract class ElasticsearchInstance extends ExternalResource {
    private static final Map<Version, ElasticsearchContainer> containersByVersion = new HashMap<>();

    private static final String DEFAULT_IMAGE_OSS = "docker.elastic.co/elasticsearch/elasticsearch-oss";
    private static final String DEFAULT_IMAGE = "elasticsearch";

    private static final int ES_PORT = 9200;
    private static final String NETWORK_ALIAS = "elasticsearch";

    private final Version version;
    protected final ElasticsearchContainer container;

    protected abstract Client client();
    protected abstract FixtureImporter fixtureImporter();

    protected static String imageNameFrom(Version version) {
        final String defaultImage = version.satisfies("^6.0.0")
                // The OSS image only exists for 6.0.0 and later
                ? DEFAULT_IMAGE_OSS
                // For older versions do not use the official image because it contains x-pack stuff we don't want
                : DEFAULT_IMAGE;
        return defaultImage + ":" + version.toString();
    }

    protected ElasticsearchInstance(String image, Version version, Network network) {
        this.version = version;

        this.container = createContainer(image, version, network);
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
                .withEnv("ES_JAVA_OPTS", "-Xms1024m -Xmx1024m")
                .withEnv("discovery.type", "single-node")
                .withEnv("action.auto_create_index", ".watches,.triggered_watches,.watcher-history-*")
                .withNetwork(network)
                .withNetworkAliases(NETWORK_ALIAS)
                .waitingFor(Wait.forHttp("/").forPort(ES_PORT));
    }

    @Override
    protected void after() {
        cleanUp();
    }

    public void cleanUp() {
        client().cleanUp();
    }

    public static String internalUri() {
        return String.format(Locale.US, "http://%s:%d", NETWORK_ALIAS, ES_PORT);
    }

    public Version version() {
        return version;
    }

    public void importFixtureResource(String resourcePath, Class<?> testClass) {
        boolean isFullResourcePath = Paths.get(resourcePath).getNameCount() > 1;

        @SuppressWarnings("UnstableApiUsage") final URL fixtureResource = isFullResourcePath
                ? Resources.getResource(resourcePath)
                : Resources.getResource(testClass, resourcePath);

        fixtureImporter().importResource(fixtureResource);

        // Make sure the data we just imported is visible
        client().refreshNode();
    }
}
