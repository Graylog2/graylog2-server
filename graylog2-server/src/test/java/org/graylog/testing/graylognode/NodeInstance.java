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
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.images.builder.dockerfile.DockerfileBuilder;

public class NodeInstance {

    private static final Logger LOG = LoggerFactory.getLogger(NodeInstance.class);

    private final GenericContainer container;

    public NodeInstance() {
        this.container = buildContainer();
    }

    public void start() {
        container.start();

        LOG.warn("container port is {}", container.getFirstMappedPort());
    }


    public GenericContainer buildContainer() {
        ImageFromDockerfile image = new ImageFromDockerfile()
                .withDockerfileFromBuilder(this::ngnixBuilder);

        return new GenericContainer(image).withExposedPorts(80);
    }

    public String ngnixBuilder(DockerfileBuilder builder) {
        return builder
                .from("alpine:3.2")
                .run("apk add --update nginx")
                .cmd("nginx", "-g", "daemon off;")
                .build();
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
