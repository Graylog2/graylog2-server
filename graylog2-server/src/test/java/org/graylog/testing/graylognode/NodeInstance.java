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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.nio.file.Paths;

public class NodeInstance {

    private static final Logger LOG = LoggerFactory.getLogger(NodeInstance.class);

    private final GenericContainer container;

    public NodeInstance(Network network, String mongoDbUri, String elasticsearchUri) {
        this.container = buildContainer(network, mongoDbUri, elasticsearchUri);
    }

    public void start() {
        container.start();

        LOG.warn("container port is {}", container.getFirstMappedPort());
    }


    private GenericContainer buildContainer(Network network, String mongoDbUri, String elasticsearchUri) {
        final String unpackedTarballDir = "/tmp/opt-graylog";

        final ImageFromDockerfile image = new ImageFromDockerfile()
                .withFileFromClasspath("Dockerfile", "org/graylog/testing/graylognode/Dockerfile")
                .withFileFromClasspath("docker-entrypoint.sh", "org/graylog/testing/graylognode/docker-entrypoint.sh")
                .withFileFromClasspath("graylog.conf", "org/graylog/testing/graylognode/config/graylog.conf")
                .withFileFromClasspath("log4j2.xml", "org/graylog/testing/graylognode/config/log4j2.xml")
                //TODO: manually assembled for now
                // - how do we prepare the graylog directory?
                // - how do we find the latest jars?
                .withFileFromPath(".", Paths.get(unpackedTarballDir));

        return new GenericContainer<>(image)
                .withExposedPorts(80)
                .withNetwork(network)
                .withEnv("GRAYLOG_MONGODB_URI", mongoDbUri)
                .withEnv("GRAYLOG_ELASTICSEARCH_HOSTS", elasticsearchUri)
                .waitingFor(Wait.forHttp("/api"))
                ;
    }

    public void stop() {
        container.stop();
    }

    public int getPort() {
        return container.getFirstMappedPort();
    }

    public String getApiAddress() {
        return "http://" + container.getContainerIpAddress() + ":" + container.getFirstMappedPort();
    }
}
