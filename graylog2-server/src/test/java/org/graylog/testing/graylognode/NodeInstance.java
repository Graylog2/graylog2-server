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
package org.graylog.testing.graylognode;

import org.graylog.testing.PropertyLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class NodeInstance {

    private static final Logger LOG = LoggerFactory.getLogger(NodeInstance.class);

    private final GenericContainer container;

    public static NodeInstance createStarted(Network network, String mongoDbUri, String elasticsearchUri) {
        NodeInstance instance = new NodeInstance(network, mongoDbUri, elasticsearchUri);
        instance.container.start();
        return instance;
    }

    public NodeInstance(Network network, String mongoDbUri, String elasticsearchUri) {
        this.container = buildContainer(network, mongoDbUri, elasticsearchUri);
    }

    private GenericContainer buildContainer(Network network, String mongoDbUri, String elasticsearchUri) {
        final ImageFromDockerfile image = new ImageFromDockerfile()
                .withFileFromClasspath("Dockerfile", "org/graylog/testing/graylognode/Dockerfile")
                .withFileFromClasspath("docker-entrypoint.sh", "org/graylog/testing/graylognode/docker-entrypoint.sh")
                .withFileFromClasspath("graylog.conf", "org/graylog/testing/graylognode/config/graylog.conf")
                .withFileFromClasspath("log4j2.xml", "org/graylog/testing/graylognode/config/log4j2.xml")
                .withFileFromPath("sigar", pathTo("sigar_dir"))
                .withFileFromPath("graylog.jar", pathTo("server_jar"))
                .withFileFromPath("graylog-plugin-aws.jar", pathTo("aws_plugin_jar"))
                .withFileFromPath("graylog-plugin-threatintel.jar", pathTo("threatintel_plugin_jar"))
                .withFileFromPath("graylog-plugin-collector.jar", pathTo("collector_plugin_jar"));

        return new GenericContainer<>(image)
                .withExposedPorts(9000)
                .withNetwork(network)
                .withEnv("GRAYLOG_MONGODB_URI", mongoDbUri)
                .withEnv("GRAYLOG_ELASTICSEARCH_HOSTS", elasticsearchUri)
                .waitingFor(Wait.forHttp("/api"));
    }

    private Path pathTo(String propertyName) {
        String property = PropertyLoader.get("api-it-tests.properties", propertyName);
        return Paths.get(property);
    }

    public void stop() {
        container.stop();
    }

    public int getPort() {
        return container.getFirstMappedPort();
    }

    public String getApiAddress() {
        return String.format(Locale.US, container.getContainerIpAddress(), container.getFirstMappedPort());
    }
}
